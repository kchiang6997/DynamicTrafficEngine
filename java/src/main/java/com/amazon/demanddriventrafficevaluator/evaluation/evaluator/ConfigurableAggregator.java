// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.evaluation.evaluator;

import com.amazon.demanddriventrafficevaluator.evaluation.experiment.ExperimentContext;
import com.amazon.demanddriventrafficevaluator.repository.entity.AggregationNode;
import com.amazon.demanddriventrafficevaluator.repository.entity.ExperimentDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;

/**
 * Aggregates model evaluation results using a configurable AND/OR aggregation tree.
 * <p>
 * This aggregator evaluates an {@link AggregationNode} tree defined in the experiment's
 * aggregation schema. The evaluation rules are:
 * <ul>
 *     <li><b>Leaf node</b>: returns the model's score (or 1.0 if the model is missing/failed)</li>
 *     <li><b>OR node</b>: returns 0.0 (filter) if any child returns 0.0</li>
 *     <li><b>AND node</b>: returns 0.0 (filter) only if all children return 0.0</li>
 * </ul>
 * </p>
 */
@Log4j2
public class ConfigurableAggregator implements ModelEvaluationResultsAggregator {

    private static final String EXPERIMENT_TYPE_SOFT_FILTER = "soft-filter";
    private static final double FALLBACK_AGGREGATED_SCORE = 1.0;
    private static final double FILTER_SCORE = 0.0;
    private static final double FORWARD_SCORE = 1.0;
    private static final String AGGREGATION_TYPE_CONFIGURABLE = "configurable";
    private static final int MAX_DEPTH = 5;

    @Override
    public AggregatedModelEvaluationResult aggregate(EvaluationContext context) {
        String experimentName = "UnknownExperiment";
        String treatmentCode = "UnknownTreatmentCode";
        double aggregatedScore = FALLBACK_AGGREGATED_SCORE;
        double aggregatedScoreWithTreatment = FALLBACK_AGGREGATED_SCORE;
        try {
            ExperimentContext experimentContext = context.getExperimentContext();
            ExperimentDefinition experimentDefinition = experimentContext.getExperimentDefinitionByType(EXPERIMENT_TYPE_SOFT_FILTER);
            experimentName = experimentDefinition.getName();

            AggregationNode aggregationSchema = experimentDefinition.getAggregationSchema();
            if (aggregationSchema == null) {
                // Fall back to max aggregation behavior
                return new ModelEvaluationResultsMaxAggregator().aggregate(context);
            }

            // Build model identifier → score map from successful evaluations
            Map<String, Double> modelScores = buildModelScoreMap(context.getModelEvaluatorOutputs());

            // Evaluate the aggregation tree
            aggregatedScore = evaluateNode(aggregationSchema, modelScores, 0);

            int treatmentCodeInInt = experimentContext.getTreatmentCodeInInt(experimentName);
            log.debug("Treatment code in int: {}", treatmentCodeInInt);
            aggregatedScoreWithTreatment = Math.max(aggregatedScore, treatmentCodeInInt);
            treatmentCode = experimentContext.getTreatmentCode(experimentName);

            return AggregatedModelEvaluationResult.builder()
                    .experimentName(experimentName)
                    .experimentType(EXPERIMENT_TYPE_SOFT_FILTER)
                    .treatmentCode(treatmentCode)
                    .treatmentCodeInInt(treatmentCodeInInt)
                    .score(aggregatedScore)
                    .scoreWithTreatment(aggregatedScoreWithTreatment)
                    .aggregationType(AGGREGATION_TYPE_CONFIGURABLE)
                    .build();
        } catch (Exception e) {
            context.addError("Failed to aggregate model evaluation results.\n" + e.getMessage());
            log.error("Failed to aggregate model evaluation results", e);
            return AggregatedModelEvaluationResult.builder()
                    .experimentName(experimentName)
                    .experimentType(EXPERIMENT_TYPE_SOFT_FILTER)
                    .treatmentCode(treatmentCode)
                    .score(aggregatedScore)
                    .scoreWithTreatment(aggregatedScoreWithTreatment)
                    .aggregationType(AGGREGATION_TYPE_CONFIGURABLE)
                    .build();
        }
    }

    /**
     * Builds a map of model identifier to score from the list of model evaluator outputs.
     * Only SUCCESS status outputs are included; failed/missing models default to 1.0 (forward).
     */
    Map<String, Double> buildModelScoreMap(List<ModelEvaluatorOutput> outputs) {
        Map<String, Double> scoreMap = new HashMap<>();
        if (outputs == null) {
            return scoreMap;
        }
        for (ModelEvaluatorOutput output : outputs) {
            if (output.getStatus() == ModelEvaluationStatus.SUCCESS) {
                scoreMap.put(output.getModelDefinition().getIdentifier(), output.getModelResult().getValue());
            } else {
                log.warn("Model [{}] evaluation did not succeed (status: {}), defaulting to forward (1.0)",
                        output.getModelDefinition().getIdentifier(), output.getStatus());
            }
        }
        return scoreMap;
    }

    /**
     * Recursively evaluates an aggregation node tree.
     * <ul>
     *     <li>Leaf: returns the model score from the map, or 1.0 (forward) if missing</li>
     *     <li>OR: returns 0.0 if any child is 0.0</li>
     *     <li>AND: returns 0.0 only if all children are 0.0</li>
     * </ul>
     * Enforces a maximum recursion depth of {@link #MAX_DEPTH}. If exceeded, returns
     * FORWARD_SCORE to prevent stack overflow from misconfigured deeply nested schemas.
     */
    double evaluateNode(AggregationNode node, Map<String, Double> modelScores) {
        return evaluateNode(node, modelScores, 0);
    }

    private double evaluateNode(AggregationNode node, Map<String, Double> modelScores, int depth) {
        if (depth >= MAX_DEPTH) {
            log.warn("Max aggregation depth ({}) reached, defaulting to forward", MAX_DEPTH);
            return FORWARD_SCORE;
        }

        if (node.isLeaf()) {
            return modelScores.getOrDefault(node.getModelIdentifier(), FORWARD_SCORE);
        }

        List<AggregationNode> conditions = node.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return FORWARD_SCORE;
        }

        if (node.getOperator() == null) {
            log.warn("Non-leaf node has null operator, defaulting to forward");
            return FORWARD_SCORE;
        }

        switch (node.getOperator()) {
            case OR:
                // OR: filter (0.0) if ANY child filters (0.0)
                for (AggregationNode child : conditions) {
                    double childScore = evaluateNode(child, modelScores, depth + 1);
                    if (childScore == FILTER_SCORE) {
                        return FILTER_SCORE;
                    }
                }
                return FORWARD_SCORE;
            case AND:
                // AND: filter (0.0) only if ALL children filter (0.0)
                for (AggregationNode child : conditions) {
                    double childScore = evaluateNode(child, modelScores, depth + 1);
                    if (childScore != FILTER_SCORE) {
                        return FORWARD_SCORE;
                    }
                }
                return FILTER_SCORE;
            default:
                log.warn("Unknown aggregation operator: {}, defaulting to forward", node.getOperator());
                return FORWARD_SCORE;
        }
    }
}
