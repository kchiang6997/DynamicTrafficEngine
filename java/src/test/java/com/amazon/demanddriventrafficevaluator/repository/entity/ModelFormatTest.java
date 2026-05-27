// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.repository.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelFormatTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @EnumSource(ModelFormat.class)
    void testJsonSerializationRoundTrip(ModelFormat format) throws JsonProcessingException {
        // Arrange
        String json = objectMapper.writeValueAsString(format);

        // Act
        ModelFormat deserialized = objectMapper.readValue(json, ModelFormat.class);

        // Assert
        assertEquals(format, deserialized);
    }

    @Test
    void testFromString_RuleBased() {
        // Act
        ModelFormat result = ModelFormat.fromString("RULE_BASED");

        // Assert
        assertEquals(ModelFormat.RULE_BASED, result);
    }

    @Test
    void testFromString_BloomFilter() {
        // Act
        ModelFormat result = ModelFormat.fromString("BLOOM_FILTER");

        // Assert
        assertEquals(ModelFormat.BLOOM_FILTER, result);
    }

    @Test
    void testFromString_InvalidValue() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> ModelFormat.fromString("INVALID"));
    }

    @Test
    void testGetValue_RuleBased() {
        // Act & Assert
        assertEquals("RULE_BASED", ModelFormat.RULE_BASED.getValue());
    }

    @Test
    void testGetValue_BloomFilter() {
        // Act & Assert
        assertEquals("BLOOM_FILTER", ModelFormat.BLOOM_FILTER.getValue());
    }

    @Test
    void testJsonSerializationValue_RuleBased() throws JsonProcessingException {
        // Act
        String json = objectMapper.writeValueAsString(ModelFormat.RULE_BASED);

        // Assert
        assertEquals("\"RULE_BASED\"", json);
    }

    @Test
    void testJsonSerializationValue_BloomFilter() throws JsonProcessingException {
        // Act
        String json = objectMapper.writeValueAsString(ModelFormat.BLOOM_FILTER);

        // Assert
        assertEquals("\"BLOOM_FILTER\"", json);
    }
}
