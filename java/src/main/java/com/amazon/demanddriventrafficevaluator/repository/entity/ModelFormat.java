// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.repository.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ModelFormat {
    RULE_BASED,
    BLOOM_FILTER;

    @JsonCreator
    public static ModelFormat fromString(String value) {
        return ModelFormat.valueOf(value);
    }

    @JsonValue
    public String getValue() {
        return this.toString();
    }
}
