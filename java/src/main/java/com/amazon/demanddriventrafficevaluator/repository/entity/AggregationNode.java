// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.repository.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a node in an aggregation schema tree.
 * <p>
 * An aggregation node is either:
 * <ul>
 *     <li>An <b>operator node</b> with a non-null {@code operator} (AND/OR) and a list of child {@code conditions}.</li>
 *     <li>A <b>leaf node</b> with a non-null {@code modelIdentifier} referencing a specific model.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AggregationNode {

    @JsonProperty("operator")
    private AggregationOperator operator;

    @JsonProperty("conditions")
    private List<AggregationNode> conditions;

    @JsonProperty("modelIdentifier")
    private String modelIdentifier;

    /**
     * Returns {@code true} if this node is a leaf (references a model identifier, no operator).
     */
    public boolean isLeaf() {
        return operator == null && modelIdentifier != null;
    }
}
