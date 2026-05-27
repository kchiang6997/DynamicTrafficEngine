// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.repository.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Property 7: AggregationNode serialization round trip
 * <p>
 * For any valid AggregationNode tree, serializing to JSON and deserializing back SHALL produce
 * an equivalent AggregationNode tree (same operator, same conditions, same modelIdentifier values
 * at each node).
 * <p>
 * <b>Validates: Requirements 6.1, 6.2, 6.3</b>
 */
class AggregationNodeSerializationPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest(name = "{0}")
    @MethodSource("aggregationNodeTrees")
    void aggregationNodeSerializationRoundTrip(String description, AggregationNode original) throws Exception {
        // Act
        String json = objectMapper.writeValueAsString(original);
        AggregationNode deserialized = objectMapper.readValue(json, AggregationNode.class);

        // Assert
        assertEquals(original, deserialized,
                "Serialization round trip failed for: " + description + "\nJSON: " + json);
    }

    static Stream<Arguments> aggregationNodeTrees() {
        return Stream.of(
                // Single leaf node
                Arguments.of(
                        "Single leaf node",
                        createLeaf("modelA")
                ),
                // Flat OR with two leaves
                Arguments.of(
                        "Flat OR with two leaves",
                        createOperatorNode(AggregationOperator.OR, "modelA", "modelB")
                ),
                // Flat AND with two leaves
                Arguments.of(
                        "Flat AND with two leaves",
                        createOperatorNode(AggregationOperator.AND, "modelA", "modelB")
                ),
                // Nested OR→AND: OR(modelA, AND(modelB, modelC))
                Arguments.of(
                        "Nested OR containing AND",
                        createNestedOrAnd()
                ),
                // Nested AND→OR: AND(modelA, OR(modelB, modelC))
                Arguments.of(
                        "Nested AND containing OR",
                        createNestedAndOr()
                ),
                // Deep nesting: OR(AND(modelA, modelB), AND(modelC, OR(modelD, modelE)))
                Arguments.of(
                        "Deep nesting with multiple levels",
                        createDeepNesting()
                ),
                // Wide tree: OR with many leaves
                Arguments.of(
                        "Wide OR with four leaves",
                        createOperatorNode(AggregationOperator.OR, "m1", "m2", "m3", "m4")
                ),
                // Operator node with single child
                Arguments.of(
                        "OR with single leaf child",
                        createOperatorNode(AggregationOperator.OR, "modelA")
                )
        );
    }

    // Helper methods

    private static AggregationNode createLeaf(String modelIdentifier) {
        AggregationNode leaf = new AggregationNode();
        leaf.setModelIdentifier(modelIdentifier);
        return leaf;
    }

    private static AggregationNode createOperatorNode(AggregationOperator operator, String... modelIds) {
        AggregationNode node = new AggregationNode();
        node.setOperator(operator);
        node.setConditions(Arrays.stream(modelIds)
                .map(AggregationNodeSerializationPropertyTest::createLeaf)
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
     * Creates: OR(AND(modelA, modelB), AND(modelC, OR(modelD, modelE)))
     */
    private static AggregationNode createDeepNesting() {
        AggregationNode and1 = new AggregationNode();
        and1.setOperator(AggregationOperator.AND);
        and1.setConditions(Arrays.asList(createLeaf("modelA"), createLeaf("modelB")));

        AggregationNode innerOr = new AggregationNode();
        innerOr.setOperator(AggregationOperator.OR);
        innerOr.setConditions(Arrays.asList(createLeaf("modelD"), createLeaf("modelE")));

        AggregationNode and2 = new AggregationNode();
        and2.setOperator(AggregationOperator.AND);
        and2.setConditions(Arrays.asList(createLeaf("modelC"), innerOr));

        AggregationNode root = new AggregationNode();
        root.setOperator(AggregationOperator.OR);
        root.setConditions(Arrays.asList(and1, and2));
        return root;
    }
}
