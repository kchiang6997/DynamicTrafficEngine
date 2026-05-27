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

class S3PathModeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @EnumSource(S3PathMode.class)
    void testJsonSerializationRoundTrip(S3PathMode mode) throws JsonProcessingException {
        // Arrange
        String json = objectMapper.writeValueAsString(mode);

        // Act
        S3PathMode deserialized = objectMapper.readValue(json, S3PathMode.class);

        // Assert
        assertEquals(mode, deserialized);
    }

    @Test
    void testFromString_Dynamic() {
        // Act
        S3PathMode result = S3PathMode.fromString("DYNAMIC");

        // Assert
        assertEquals(S3PathMode.DYNAMIC, result);
    }

    @Test
    void testFromString_Static() {
        // Act
        S3PathMode result = S3PathMode.fromString("STATIC");

        // Assert
        assertEquals(S3PathMode.STATIC, result);
    }

    @Test
    void testFromString_InvalidValue() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> S3PathMode.fromString("INVALID"));
    }

    @Test
    void testGetValue_Dynamic() {
        // Act & Assert
        assertEquals("DYNAMIC", S3PathMode.DYNAMIC.getValue());
    }

    @Test
    void testGetValue_Static() {
        // Act & Assert
        assertEquals("STATIC", S3PathMode.STATIC.getValue());
    }

    @Test
    void testJsonSerializationValue_Dynamic() throws JsonProcessingException {
        // Act
        String json = objectMapper.writeValueAsString(S3PathMode.DYNAMIC);

        // Assert
        assertEquals("\"DYNAMIC\"", json);
    }

    @Test
    void testJsonSerializationValue_Static() throws JsonProcessingException {
        // Act
        String json = objectMapper.writeValueAsString(S3PathMode.STATIC);

        // Assert
        assertEquals("\"STATIC\"", json);
    }
}
