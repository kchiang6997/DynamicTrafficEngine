// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.evaluation.evaluator;

import com.amazon.demanddriventrafficevaluator.repository.entity.ModelDefinition;
import com.amazon.demanddriventrafficevaluator.repository.entity.ModelFormat;
import com.amazon.demanddriventrafficevaluator.repository.entity.ModelResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DelegatingModelEvaluator}.
 * <p>
 * Tests routing of RULE_BASED to RuleBasedModelEvaluator,
 * BLOOM_FILTER to BloomFilterModelEvaluator, and unknown format
 * fallback to the default (RULE_BASED) evaluator.
 * </p>
 * <p>
 * Requirements: 8.3, 8.4
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class DelegatingModelEvaluatorTest {

    @Mock
    private ModelEvaluator ruleBasedEvaluator;
    @Mock
    private ModelEvaluator bloomFilterEvaluator;

    private DelegatingModelEvaluator delegatingEvaluator;

    @BeforeEach
    void setUp() {
        delegatingEvaluator = new DelegatingModelEvaluator(
                Map.of(
                        ModelFormat.RULE_BASED, ruleBasedEvaluator,
                        ModelFormat.BLOOM_FILTER, bloomFilterEvaluator
                )
        );
    }

    @Test
    void evaluate_routesToRuleBasedEvaluator_whenModelFormatIsRuleBased() {
        // Arrange
        ModelEvaluatorInput input = createInputWithFormat(ModelFormat.RULE_BASED);
        ModelEvaluatorOutput expectedOutput = createOutput("rule_model", ModelEvaluationStatus.SUCCESS, 0.5);
        when(ruleBasedEvaluator.evaluate(any())).thenReturn(expectedOutput);

        // Act
        ModelEvaluatorOutput result = delegatingEvaluator.evaluate(input);

        // Assert
        assertNotNull(result);
        assertEquals(expectedOutput, result);
        verify(ruleBasedEvaluator).evaluate(input);
        verifyNoInteractions(bloomFilterEvaluator);
    }

    @Test
    void evaluate_routesToBloomFilterEvaluator_whenModelFormatIsBloomFilter() {
        // Arrange
        ModelEvaluatorInput input = createInputWithFormat(ModelFormat.BLOOM_FILTER);
        ModelEvaluatorOutput expectedOutput = createOutput("bloom_model", ModelEvaluationStatus.SUCCESS, 0.0);
        when(bloomFilterEvaluator.evaluate(any())).thenReturn(expectedOutput);

        // Act
        ModelEvaluatorOutput result = delegatingEvaluator.evaluate(input);

        // Assert
        assertNotNull(result);
        assertEquals(expectedOutput, result);
        verify(bloomFilterEvaluator).evaluate(input);
        verifyNoInteractions(ruleBasedEvaluator);
    }

    @Test
    void evaluate_fallsBackToRuleBasedEvaluator_whenModelFormatIsUnknown() {
        // Arrange
        // Create a delegating evaluator with only RULE_BASED registered
        // to simulate an unknown format falling back to default
        DelegatingModelEvaluator evaluatorWithLimitedRegistry = new DelegatingModelEvaluator(
                Map.of(ModelFormat.RULE_BASED, ruleBasedEvaluator)
        );
        ModelEvaluatorInput input = createInputWithFormat(ModelFormat.BLOOM_FILTER);
        ModelEvaluatorOutput expectedOutput = createOutput("fallback_model", ModelEvaluationStatus.SUCCESS, 1.0);
        when(ruleBasedEvaluator.evaluate(any())).thenReturn(expectedOutput);

        // Act
        ModelEvaluatorOutput result = evaluatorWithLimitedRegistry.evaluate(input);

        // Assert
        assertNotNull(result);
        assertEquals(expectedOutput, result);
        verify(ruleBasedEvaluator).evaluate(input);
    }

    @Test
    void getFeatures_routesToRuleBasedEvaluator_whenModelFormatIsRuleBased() {
        // Arrange
        ModelEvaluatorInput input = createInputWithFormat(ModelFormat.RULE_BASED);
        when(ruleBasedEvaluator.getFeatures(any())).thenReturn(java.util.Collections.emptyList());

        // Act
        delegatingEvaluator.getFeatures(input);

        // Assert
        verify(ruleBasedEvaluator).getFeatures(input);
        verifyNoInteractions(bloomFilterEvaluator);
    }

    @Test
    void getFeatures_routesToBloomFilterEvaluator_whenModelFormatIsBloomFilter() {
        // Arrange
        ModelEvaluatorInput input = createInputWithFormat(ModelFormat.BLOOM_FILTER);
        when(bloomFilterEvaluator.getFeatures(any())).thenReturn(java.util.Collections.emptyList());

        // Act
        delegatingEvaluator.getFeatures(input);

        // Assert
        verify(bloomFilterEvaluator).getFeatures(input);
        verifyNoInteractions(ruleBasedEvaluator);
    }

    @Test
    void evaluate_defaultFormatIsRuleBased_whenModelDefinitionHasNoExplicitFormat() {
        // Arrange
        ModelDefinition modelDefinition = new ModelDefinition();
        modelDefinition.setIdentifier("default_format_model");
        // modelFormat defaults to RULE_BASED when not explicitly set
        ModelEvaluatorInput input = ModelEvaluatorInput.builder()
                .modelDefinition(modelDefinition)
                .build();
        ModelEvaluatorOutput expectedOutput = createOutput("default_format_model", ModelEvaluationStatus.SUCCESS, 1.0);
        when(ruleBasedEvaluator.evaluate(any())).thenReturn(expectedOutput);

        // Act
        ModelEvaluatorOutput result = delegatingEvaluator.evaluate(input);

        // Assert
        assertNotNull(result);
        assertEquals(expectedOutput, result);
        verify(ruleBasedEvaluator).evaluate(input);
        verifyNoInteractions(bloomFilterEvaluator);
    }

    private ModelEvaluatorInput createInputWithFormat(ModelFormat format) {
        ModelDefinition modelDefinition = new ModelDefinition();
        modelDefinition.setIdentifier("test_model");
        modelDefinition.setModelFormat(format);
        return ModelEvaluatorInput.builder()
                .modelDefinition(modelDefinition)
                .build();
    }

    private ModelEvaluatorOutput createOutput(String modelId, ModelEvaluationStatus status, double value) {
        ModelDefinition modelDefinition = new ModelDefinition();
        modelDefinition.setIdentifier(modelId);
        ModelResult modelResult = ModelResult.builder().value(value).build();
        return ModelEvaluatorOutput.builder()
                .modelDefinition(modelDefinition)
                .modelResult(modelResult)
                .status(status)
                .build();
    }
}
