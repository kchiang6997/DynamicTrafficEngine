// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.repository.loader.model;

import com.amazon.demanddriventrafficevaluator.repository.entity.ModelValueType;
import com.amazon.demanddriventrafficevaluator.repository.entity.S3PathMode;
import com.amazon.demanddriventrafficevaluator.repository.loader.LoaderInput;
import lombok.Getter;

@Getter
public class ModelResultLoaderInput extends LoaderInput {

    private final String modelIdentifier;
    private final ModelValueType modelType;
    private final S3PathMode s3PathMode;

    public ModelResultLoaderInput(String s3Bucket, String s3ObjectKey, String vendor,
                                  String modelIdentifier, ModelValueType modelType) {
        this(s3Bucket, s3ObjectKey, vendor, modelIdentifier, modelType, S3PathMode.DYNAMIC);
    }

    public ModelResultLoaderInput(String s3Bucket, String s3ObjectKey, String vendor,
                                  String modelIdentifier, ModelValueType modelType,
                                  S3PathMode s3PathMode) {
        super(s3Bucket, s3ObjectKey, "model-result", vendor);
        this.modelIdentifier = modelIdentifier;
        this.modelType = modelType;
        this.s3PathMode = s3PathMode;
    }
}
