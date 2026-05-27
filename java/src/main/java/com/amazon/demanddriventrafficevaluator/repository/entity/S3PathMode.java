// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.repository.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum S3PathMode {
    DYNAMIC,
    STATIC;

    @JsonCreator
    public static S3PathMode fromString(String value) {
        return S3PathMode.valueOf(value);
    }

    @JsonValue
    public String getValue() {
        return this.toString();
    }
}
