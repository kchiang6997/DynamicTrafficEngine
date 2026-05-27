// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.repository.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Property 9: Backward compatibility for existing configurations.
 * Verifies that ModelDefinition JSON without new fields (modelFormat, s3PathMode)
 * deserializes with correct defaults: modelFormat == RULE_BASED and s3PathMode == DYNAMIC.
 *
 * <p><b>Validates: Requirements 8.2, 8.8, 12.1, 12.3</b></p>
 */
class ModelDefinitionBackwardCompatibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    static Stream<String> existingModelDefinitionJsonStrings() {
        return Stream.of(
                // Minimal ModelDefinition with only identifier
                "{\"identifier\": \"model_v1\"}",

                // ModelDefinition with modelType but no modelFormat or s3PathMode
                "{\"identifier\": \"adsp_low-value_v2\", \"dsp\": \"adsp\", "
                        + "\"name\": \"low-value\", \"version\": \"v2\", "
                        + "\"modelType\": \"LowValue\"}",

                // ModelDefinition with HighValue modelType
                "{\"identifier\": \"adsp_high-value_v1\", \"dsp\": \"adsp\", "
                        + "\"name\": \"high-value\", \"version\": \"v1\", "
                        + "\"modelType\": \"HighValue\"}",

                // ModelDefinition with features but no new fields
                "{\"identifier\": \"adsp_low-value_v2\", \"dsp\": \"adsp\", "
                        + "\"name\": \"low-value\", \"version\": \"v2\", "
                        + "\"modelType\": \"LowValue\", "
                        + "\"featureExtractorType\": \"JsonExtractor\", "
                        + "\"features\": [{\"name\": \"publisherId\", "
                        + "\"fields\": [\"$.site.publisher.id\"], "
                        + "\"transformation\": [\"GetFirstNotEmpty\"]}]}",

                // Empty JSON object (all fields absent)
                "{}"
        );
    }

    @ParameterizedTest
    @MethodSource("existingModelDefinitionJsonStrings")
    void testDeserializationDefaultsWhenNewFieldsAbsent(String json) throws JsonProcessingException {
        // Act
        ModelDefinition modelDefinition = objectMapper.readValue(json, ModelDefinition.class);

        // Assert
        assertNotNull(modelDefinition);
        assertEquals(ModelFormat.RULE_BASED, modelDefinition.getModelFormat(),
                "modelFormat should default to RULE_BASED when absent");
        assertEquals(S3PathMode.DYNAMIC, modelDefinition.getS3PathMode(),
                "s3PathMode should default to DYNAMIC when absent");
    }
}
