// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.evaluation.evaluator;

import com.amazon.demanddriventrafficevaluator.modelfeature.Extraction;
import com.amazon.demanddriventrafficevaluator.modelfeature.FeatureExtractorType;
import com.amazon.demanddriventrafficevaluator.modelfeature.Transformation;
import com.amazon.demanddriventrafficevaluator.repository.entity.FeatureConfiguration;
import com.amazon.demanddriventrafficevaluator.repository.entity.ModelDefinition;
import com.amazon.demanddriventrafficevaluator.repository.provider.model.ModelResultProvider;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Property 8: Default-forward on evaluation error.
 * <p>
 * For any ModelEvaluator implementation (RuleBasedModelEvaluator or BloomFilterModelEvaluator),
 * when the evaluate method encounters an exception during feature extraction or model lookup,
 * the returned ModelEvaluatorOutput SHALL have status ERROR.
 * </p>
 * <p>
 * <b>Validates: Requirements 4.4, 11.1, 11.2</b>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class DefaultForwardOnEvaluationErrorPropertyTest {

    @Mock
    private Extraction extraction;
    @Mock
    private Transformation transformation;
    @Mock
    private ModelResultProvider ruleBasedModelResultProvider;
    @Mock
    private ModelResultProvider bloomFilterModelResultProvider;
    @Mock
    private OpenRtbRequestContext openRtbRequestContext;

    /**
     * Provides different exception types to test that all exceptions result in ERROR status.
     */
    static Stream<Exception> exceptionProvider() {
        return Stream.of(
                new RuntimeException("Simulated runtime error"),
                new NullPointerException("Simulated null pointer"),
                new IllegalArgumentException("Simulated illegal argument"),
                new IllegalStateException("Simulated illegal state"),
                new UnsupportedOperationException("Simulated unsupported operation")
        );
    }

    private ModelEvaluatorInput createInput() {
        ModelDefinition modelDefinition = new ModelDefinition();
        modelDefinition.setIdentifier("test_model_v1");
        modelDefinition.setFeatureExtractorType(FeatureExtractorType.JsonExtractor);
        FeatureConfiguration featureConfig = new FeatureConfiguration();
        featureConfig.setName("testFeature");
        featureConfig.setFields(Collections.singletonList("$.site.publisher.id"));
        modelDefinition.setFeatures(Collections.singletonList(featureConfig));

        EvaluationContext evaluationContext = new EvaluationContext();
        evaluationContext.setOpenRtbRequestContext(openRtbRequestContext);
        ModelEvaluationContext context = new ModelEvaluationContext(evaluationContext);

        return ModelEvaluatorInput.builder()
                .context(context)
                .modelDefinition(modelDefinition)
                .build();
    }

    /**
     * Validates: Requirements 4.4, 11.1, 11.2
     *
     * RuleBasedModelEvaluator returns ERROR status when extraction throws any exception.
     */
    @ParameterizedTest(name = "RuleBasedModelEvaluator returns ERROR on extraction exception: {0}")
    @MethodSource("exceptionProvider")
    void ruleBasedEvaluator_returnsErrorStatus_whenExtractionThrows(Exception exception) {
        // Arrange
        when(extraction.extract(any(), any(), any())).thenThrow(exception);
        RuleBasedModelEvaluator evaluator = new RuleBasedModelEvaluator(
                extraction, transformation, ruleBasedModelResultProvider);
        ModelEvaluatorInput input = createInput();

        // Act
        ModelEvaluatorOutput output = evaluator.evaluate(input);

        // Assert
        assertNotNull(output);
        assertEquals(ModelEvaluationStatus.ERROR, output.getStatus());
    }

    /**
     * Validates: Requirements 4.4, 11.1, 11.2
     *
     * BloomFilterModelEvaluator returns ERROR status when extraction throws any exception.
     */
    @ParameterizedTest(name = "BloomFilterModelEvaluator returns ERROR on extraction exception: {0}")
    @MethodSource("exceptionProvider")
    void bloomFilterEvaluator_returnsErrorStatus_whenExtractionThrows(Exception exception) {
        // Arrange
        when(extraction.extract(any(), any(), any())).thenThrow(exception);
        BloomFilterModelEvaluator evaluator = new BloomFilterModelEvaluator(
                extraction, transformation, bloomFilterModelResultProvider);
        ModelEvaluatorInput input = createInput();

        // Act
        ModelEvaluatorOutput output = evaluator.evaluate(input);

        // Assert
        assertNotNull(output);
        assertEquals(ModelEvaluationStatus.ERROR, output.getStatus());
    }

    /**
     * Validates: Requirements 4.4, 11.1
     *
     * RuleBasedModelEvaluator returns ERROR status when model result provider throws any exception.
     */
    @ParameterizedTest(name = "RuleBasedModelEvaluator returns ERROR on provider exception: {0}")
    @MethodSource("exceptionProvider")
    void ruleBasedEvaluator_returnsErrorStatus_whenProviderThrows(Exception exception) {
        // Arrange
        when(extraction.extract(any(), any(), any())).thenReturn(
                com.amazon.demanddriventrafficevaluator.modelfeature.ModelFeature.builder()
                        .values(Collections.singletonList("testValue"))
                        .build());
        when(transformation.transform(any())).thenReturn(
                com.amazon.demanddriventrafficevaluator.modelfeature.ModelFeature.builder()
                        .values(Collections.singletonList("testValue"))
                        .build());
        when(ruleBasedModelResultProvider.provide(any())).thenThrow(exception);
        RuleBasedModelEvaluator evaluator = new RuleBasedModelEvaluator(
                extraction, transformation, ruleBasedModelResultProvider);
        ModelEvaluatorInput input = createInput();

        // Act
        ModelEvaluatorOutput output = evaluator.evaluate(input);

        // Assert
        assertNotNull(output);
        assertEquals(ModelEvaluationStatus.ERROR, output.getStatus());
    }

    /**
     * Validates: Requirements 4.4, 11.2
     *
     * BloomFilterModelEvaluator returns ERROR status when model result provider throws any exception.
     */
    @ParameterizedTest(name = "BloomFilterModelEvaluator returns ERROR on provider exception: {0}")
    @MethodSource("exceptionProvider")
    void bloomFilterEvaluator_returnsErrorStatus_whenProviderThrows(Exception exception) {
        // Arrange
        when(extraction.extract(any(), any(), any())).thenReturn(
                com.amazon.demanddriventrafficevaluator.modelfeature.ModelFeature.builder()
                        .values(Collections.singletonList("testValue"))
                        .build());
        when(transformation.transform(any())).thenReturn(
                com.amazon.demanddriventrafficevaluator.modelfeature.ModelFeature.builder()
                        .values(Collections.singletonList("testValue"))
                        .build());
        when(bloomFilterModelResultProvider.provide(any())).thenThrow(exception);
        BloomFilterModelEvaluator evaluator = new BloomFilterModelEvaluator(
                extraction, transformation, bloomFilterModelResultProvider);
        ModelEvaluatorInput input = createInput();

        // Act
        ModelEvaluatorOutput output = evaluator.evaluate(input);

        // Assert
        assertNotNull(output);
        assertEquals(ModelEvaluationStatus.ERROR, output.getStatus());
    }
}
