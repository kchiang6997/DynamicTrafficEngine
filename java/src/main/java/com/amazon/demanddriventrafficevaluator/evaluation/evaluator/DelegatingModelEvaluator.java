// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.evaluation.evaluator;

import com.amazon.demanddriventrafficevaluator.modelfeature.ModelFeature;
import com.amazon.demanddriventrafficevaluator.repository.entity.ModelFormat;

import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;

/**
 * A delegating implementation of the ModelEvaluator interface that routes evaluation
 * to the correct evaluator based on the model's format.
 * <p>
 * This evaluator uses a {@code Map<ModelFormat, ModelEvaluator>} registry to dispatch
 * evaluation requests. When a model's format is not found in the registry, it falls
 * back to the {@link ModelFormat#RULE_BASED} evaluator as the default.
 * </p>
 * <p>
 * This design allows adding new model formats (e.g., ONNX, TensorFlow Lite) by simply
 * registering a new evaluator in the map — no changes to this class are needed.
 * </p>
 */
@Log4j2
public class DelegatingModelEvaluator implements ModelEvaluator {

    private final Map<ModelFormat, ModelEvaluator> evaluatorsByFormat;
    private final ModelEvaluator defaultEvaluator;

    /**
     * Creates a new DelegatingModelEvaluator with the given evaluator registry.
     * <p>
     * The default evaluator is set to the {@link ModelFormat#RULE_BASED} evaluator
     * from the registry. If no RULE_BASED evaluator is registered, the default
     * evaluator will be null and unknown formats will cause a NullPointerException.
     * </p>
     *
     * @param evaluatorsByFormat A map of model formats to their corresponding evaluators.
     */
    public DelegatingModelEvaluator(Map<ModelFormat, ModelEvaluator> evaluatorsByFormat) {
        this.evaluatorsByFormat = evaluatorsByFormat;
        this.defaultEvaluator = evaluatorsByFormat.get(ModelFormat.RULE_BASED);
    }

    /**
     * Delegates feature extraction to the appropriate evaluator based on the model's format.
     *
     * @param input The ModelEvaluatorInput containing the model definition and context.
     * @return A list of ModelFeature objects extracted by the delegated evaluator.
     */
    @Override
    public List<ModelFeature> getFeatures(ModelEvaluatorInput input) {
        ModelFormat format = input.getModelDefinition().getModelFormat();
        ModelEvaluator evaluator = evaluatorsByFormat.getOrDefault(format, defaultEvaluator);
        log.debug("Delegating getFeatures to evaluator for format: {}", format);
        return evaluator.getFeatures(input);
    }

    /**
     * Delegates evaluation to the appropriate evaluator based on the model's format.
     * <p>
     * Looks up the evaluator for the model's {@link ModelFormat} in the registry.
     * If no evaluator is found for the format, falls back to the default
     * ({@link ModelFormat#RULE_BASED}) evaluator.
     * </p>
     *
     * @param input The ModelEvaluatorInput containing the model definition and context.
     * @return A ModelEvaluatorOutput from the delegated evaluator.
     */
    @Override
    public ModelEvaluatorOutput evaluate(ModelEvaluatorInput input) {
        ModelFormat format = input.getModelDefinition().getModelFormat();
        ModelEvaluator evaluator = evaluatorsByFormat.getOrDefault(format, defaultEvaluator);
        log.debug("Delegating evaluate to evaluator for format: {}", format);
        return evaluator.evaluate(input);
    }
}
