// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.evaluation.evaluator;

import com.amazon.demanddriventrafficevaluator.repository.entity.AggregationNode;
import com.amazon.demanddriventrafficevaluator.repository.entity.AggregationOperator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Property 6: Aggregation tree boolean evaluation
 * <p>
 * For any valid AggregationNode tree and any mapping of model identifiers to scores (0.0 or 1.0),
 * the ConfigurableAggregator SHALL produce a score that matches recursive boolean evaluation where:
 * <ul>
 *     <li>A leaf node evaluates to its model's score (or 1.0 if the model is missing/failed)</li>
 *     <li>An OR node evaluates to 0.0 if any child evaluates to 0.0, otherwise 1.0</li>
 *     <li>An AND node evaluates to 0.0 only if all children evaluate to 0.0, otherwise 1.0</li>
 * </ul>
 * <p>
 * <b>Validates: Requirements 5.2, 5.3, 5.4, 5.6, 5.7</b>
 */
class AggregationTreeBooleanEvaluationPropertyTest {

    private final ConfigurableAggregator aggregator = new ConfigurableAggregator();

    @ParameterizedTest(name = "{0}")
    @MethodSource("aggregationTreeTestCases")
    void aggregationTreeBooleanEvaluation(String description, AggregationNode tree,
                                          Map<String, Double> modelScores, double expectedScore) {
        // Act
        double actualScore = aggregator.evaluateNode(tree, modelScores);

        // Assert
        assertEquals(expectedScore, actualScore, 0.001,
                "Aggregation tree evaluation mismatch for: " + description);
    }

    static Stream<Arguments> aggregationTreeTestCases() {
        return Stream.of(
                // Single leaf — model present with filter score
                Arguments.of(
                        "Single leaf - model scores 0.0 (filter)",
                        createLeaf("modelA"),
                        Map.of("modelA", 0.0),
                        0.0
                ),
                // Single leaf — model present with forward score
                Arguments.of(
                        "Single leaf - model scores 1.0 (forward)",
                        createLeaf("modelA"),
                        Map.of("modelA", 1.0),
                        1.0
                ),
                // Single leaf — model missing defaults to 1.0 (forward)
                Arguments.of(
                        "Single leaf - missing model defaults to 1.0",
                        createLeaf("modelA"),
                        Collections.emptyMap(),
                        1.0
                ),
                // Flat OR — all children forward → forward
                Arguments.of(
                        "Flat OR - all forward → 1.0",
                        createOperatorNode(AggregationOperator.OR, "modelA", "modelB"),
                        Map.of("modelA", 1.0, "modelB", 1.0),
                        1.0
                ),
                // Flat OR — one child filters → filter
                Arguments.of(
                        "Flat OR - one filter → 0.0",
                        createOperatorNode(AggregationOperator.OR, "modelA", "modelB"),
                        Map.of("modelA", 0.0, "modelB", 1.0),
                        0.0
                ),
                // Flat OR — all children filter → filter
                Arguments.of(
                        "Flat OR - all filter → 0.0",
                        createOperatorNode(AggregationOperator.OR, "modelA", "modelB"),
                        Map.of("modelA", 0.0, "modelB", 0.0),
                        0.0
                ),
                // Flat AND — all children filter → filter
                Arguments.of(
                        "Flat AND - all filter → 0.0",
                        createOperatorNode(AggregationOperator.AND, "modelA", "modelB"),
                        Map.of("modelA", 0.0, "modelB", 0.0),
                        0.0
                ),
                // Flat AND — one child forwards → forward
                Arguments.of(
                        "Flat AND - one forward → 1.0",
                        createOperatorNode(AggregationOperator.AND, "modelA", "modelB"),
                        Map.of("modelA", 0.0, "modelB", 1.0),
                        1.0
                ),
                // Flat AND — all children forward → forward
                Arguments.of(
                        "Flat AND - all forward → 1.0",
                        createOperatorNode(AggregationOperator.AND, "modelA", "modelB"),
                        Map.of("modelA", 1.0, "modelB", 1.0),
                        1.0
                ),
                // Nested OR→AND: OR(modelA, AND(modelB, modelC))
                // modelA=1.0, modelB=0.0, modelC=0.0 → AND=0.0, OR(1.0, 0.0)=0.0
                Arguments.of(
                        "Nested OR→AND - AND children both filter → OR sees 0.0 → filter",
                        createNestedOrAnd(),
                        Map.of("modelA", 1.0, "modelB", 0.0, "modelC", 0.0),
                        0.0
                ),
                // Nested OR→AND: OR(modelA, AND(modelB, modelC))
                // modelA=1.0, modelB=0.0, modelC=1.0 → AND=1.0, OR(1.0, 1.0)=1.0
                Arguments.of(
                        "Nested OR→AND - AND has one forward → AND=1.0, OR=1.0",
                        createNestedOrAnd(),
                        Map.of("modelA", 1.0, "modelB", 0.0, "modelC", 1.0),
                        1.0
                ),
                // Nested AND→OR: AND(modelA, OR(modelB, modelC))
                // modelA=0.0, modelB=1.0, modelC=0.0 → OR=0.0 (modelC filters), AND(0.0, 0.0)=0.0
                Arguments.of(
                        "Nested AND→OR - OR has one filter → OR=0.0, AND(0.0, 0.0)=0.0",
                        createNestedAndOr(),
                        Map.of("modelA", 0.0, "modelB", 1.0, "modelC", 0.0),
                        0.0
                ),
                // Nested AND→OR: AND(modelA, OR(modelB, modelC))
                // modelA=0.0, modelB=1.0, modelC=1.0 → OR=1.0 (no child filters), AND(0.0, 1.0)=1.0
                Arguments.of(
                        "Nested AND→OR - OR all forward → AND has one forward → 1.0",
                        createNestedAndOr(),
                        Map.of("modelA", 0.0, "modelB", 1.0, "modelC", 1.0),
                        1.0
                ),
                // Nested AND→OR: AND(modelA, OR(modelB, modelC))
                // modelA=0.0, modelB=0.0, modelC=0.0 → OR=0.0, AND(0.0, 0.0)=0.0
                Arguments.of(
                        "Nested AND→OR - all filter → 0.0",
                        createNestedAndOr(),
                        Map.of("modelA", 0.0, "modelB", 0.0, "modelC", 0.0),
                        0.0
                ),
                // Missing model in OR — missing defaults to 1.0
                Arguments.of(
                        "OR with missing model - missing defaults to 1.0, other filters → 0.0",
                        createOperatorNode(AggregationOperator.OR, "modelA", "modelMissing"),
                        Map.of("modelA", 0.0),
                        0.0
                ),
                // Missing model in AND — missing defaults to 1.0, so AND cannot be all-0.0
                Arguments.of(
                        "AND with missing model - missing defaults to 1.0 → 1.0",
                        createOperatorNode(AggregationOperator.AND, "modelA", "modelMissing"),
                        Map.of("modelA", 0.0),
                        1.0
                ),
                // Malformed node — null operator and null modelIdentifier with conditions → default forward
                Arguments.of(
                        "Malformed node - null operator with conditions → defaults to 1.0",
                        createMalformedNode(),
                        Map.of("modelA", 0.0),
                        1.0
                ),
                // Max depth exceeded — deeply nested tree beyond depth 5 → default forward
                Arguments.of(
                        "Max depth exceeded - nested beyond 5 levels → defaults to 1.0",
                        createDeeplyNestedTree(6),
                        Map.of("deepLeaf", 0.0),
                        1.0
                ),
                // At max depth — nested exactly at depth 5 → still evaluates correctly
                Arguments.of(
                        "At max depth boundary - nested at depth 4 → evaluates leaf",
                        createDeeplyNestedTree(4),
                        Map.of("deepLeaf", 0.0),
                        0.0
                )
        );
    }

    // Helper methods to build tree structures

    private static AggregationNode createLeaf(String modelIdentifier) {
        AggregationNode leaf = new AggregationNode();
        leaf.setModelIdentifier(modelIdentifier);
        return leaf;
    }

    private static AggregationNode createOperatorNode(AggregationOperator operator, String... modelIds) {
        AggregationNode node = new AggregationNode();
        node.setOperator(operator);
        node.setConditions(Arrays.stream(modelIds)
                .map(AggregationTreeBooleanEvaluationPropertyTest::createLeaf)
                .collect(java.util.stream.Collectors.toList()));
        return node;
    }

    /**
     * Creates: OR(modelA, AND(modelB, modelC))
     */
    private static AggregationNode createNestedOrAnd() {
        AggregationNode andNode = new AggregationNode();
        andNode.setOperator(AggregationOperator.AND);
        andNode.setConditions(Arrays.asList(createLeaf("modelB"), createLeaf("modelC")));

        AggregationNode orNode = new AggregationNode();
        orNode.setOperator(AggregationOperator.OR);
        orNode.setConditions(Arrays.asList(createLeaf("modelA"), andNode));
        return orNode;
    }

    /**
     * Creates: AND(modelA, OR(modelB, modelC))
     */
    private static AggregationNode createNestedAndOr() {
        AggregationNode orNode = new AggregationNode();
        orNode.setOperator(AggregationOperator.OR);
        orNode.setConditions(Arrays.asList(createLeaf("modelB"), createLeaf("modelC")));

        AggregationNode andNode = new AggregationNode();
        andNode.setOperator(AggregationOperator.AND);
        andNode.setConditions(Arrays.asList(createLeaf("modelA"), orNode));
        return andNode;
    }

    /**
     * Creates a malformed node: null operator, null modelIdentifier, but with conditions.
     * This simulates misconfigured JSON where the operator field is missing.
     */
    private static AggregationNode createMalformedNode() {
        AggregationNode node = new AggregationNode();
        // operator is null, modelIdentifier is null, but conditions are populated
        node.setConditions(Arrays.asList(createLeaf("modelA"), createLeaf("modelB")));
        return node;
    }

    /**
     * Creates a deeply nested OR tree with the given depth, with a leaf at the bottom.
     * Each level is an OR node wrapping the next level.
     */
    private static AggregationNode createDeeplyNestedTree(int depth) {
        AggregationNode leaf = createLeaf("deepLeaf");
        AggregationNode current = leaf;
        for (int i = 0; i < depth; i++) {
            AggregationNode parent = new AggregationNode();
            parent.setOperator(AggregationOperator.OR);
            parent.setConditions(Arrays.asList(current));
            current = parent;
        }
        return current;
    }
}
