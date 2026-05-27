// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.repository.entity;

/**
 * Logical operators for aggregation schema tree nodes.
 * <p>
 * Used in {@link AggregationNode} to define how child conditions are combined:
 * <ul>
 *     <li>{@code AND} — filter only if all children recommend filtering</li>
 *     <li>{@code OR} — filter if any child recommends filtering</li>
 * </ul>
 */
public enum AggregationOperator {
    AND,
    OR
}
