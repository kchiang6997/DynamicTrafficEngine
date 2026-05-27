// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.evaluation.evaluator;

import com.amazon.demanddriventrafficevaluator.evaluation.experiment.ExperimentContext;
import com.amazon.demanddriventrafficevaluator.repository.entity.AggregationNode;
import com.amazon.demanddriventrafficevaluator.repository.entity.AggregationOperator;
import com.amazon.demanddriventrafficevaluator.repository.entity.ExperimentDefinition;
import com.amazon.demanddriventrafficevaluator.repository.entity.ModelDefinition;
import com.amazon.demanddriventrafficevaluator.repository.entity.ModelResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigurableAggregatorTest {

    @Mock
    private EvaluationContext mockContext;
    @Mock
    private ExperimentContext mockExperimentContext;

    private ConfigurableAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new ConfigurableAggregator();
    }

    @Test
    void testTreatmentCodePreservation() {
        // Arrange
        String experimentName = "TestExperiment";
        String treatmentCode = "C";
        int treatmentCodeInInt = 1;

        AggregationNode schema = createOperatorNode(AggregationOperator.OR, "Model1");

        ExperimentDefinition experimentDefinition = ExperimentDefinition.builder()
                .name(experimentName)
                .type("soft-filter")
                .aggregationSchema(schema)
                .build();

        when(mockContext.getExperimentContext()).thenReturn(mockExperimentContext);
        when(mockExperimentContext.getExperimentDefinitionByType("soft-filter")).thenReturn(experimentDefinition);
        when(mockExperimentContext.getTreatmentCodeInInt(experimentName)).thenReturn(treatmentCodeInInt);
        when(mockExperimentContext.getTreatmentCode(experimentName)).thenReturn(treatmentCode);

        List<ModelEvaluatorOutput> outputs = List.of(
                createModelEvaluatorOutput("Model1", 0.0, ModelEvaluationStatus.SUCCESS)
        );
        when(mockContext.getModelEvaluatorOutputs()).thenReturn(outputs);

        // Act
        AggregatedModelEvaluationResult result = aggregator.aggregate(mockContext);

        // Assert
        assertNotNull(result);
        assertEquals(experimentName, result.getExperimentName());
        assertEquals("soft-filter", result.getExperimentType());
        assertEquals(treatmentCode, result.getTreatmentCode());
        assertEquals(treatmentCodeInInt, result.getTreatmentCodeInInt());
        assertEquals(0.0, result.getScore());
        // scoreWithTreatment = max(0.0, 1) = 1.0
        assertEquals(1.0, result.getScoreWithTreatment());
        assertEquals("configurable", result.getAggregationType());
    }

    @Test
    void testTreatmentCodePreservationWithTreatmentGroup() {
        // Arrange — treatment group "T" has treatmentCodeInInt = 0
        String experimentName = "TestExperiment";
        String treatmentCode = "T";
        int treatmentCodeInInt = 0;

        AggregationNode schema = createOperatorNode(AggregationOperator.OR, "Model1");

        ExperimentDefinition experimentDefinition = ExperimentDefinition.builder()
                .name(experimentName)
                .type("soft-filter")
                .aggregationSchema(schema)
                .build();

        when(mockContext.getExperimentContext()).thenReturn(mockExperimentContext);
        when(mockExperimentContext.getExperimentDefinitionByType("soft-filter")).thenReturn(experimentDefinition);
        when(mockExperimentContext.getTreatmentCodeInInt(experimentName)).thenReturn(treatmentCodeInInt);
        when(mockExperimentContext.getTreatmentCode(experimentName)).thenReturn(treatmentCode);

        List<ModelEvaluatorOutput> outputs = List.of(
                createModelEvaluatorOutput("Model1", 0.0, ModelEvaluationStatus.SUCCESS)
        );
        when(mockContext.getModelEvaluatorOutputs()).thenReturn(outputs);

        // Act
        AggregatedModelEvaluationResult result = aggregator.aggregate(mockContext);

        // Assert
        assertNotNull(result);
        assertEquals(treatmentCode, result.getTreatmentCode());
        assertEquals(treatmentCodeInInt, result.getTreatmentCodeInInt());
        assertEquals(0.0, result.getScore());
        // scoreWithTreatment = max(0.0, 0) = 0.0
        assertEquals(0.0, result.getScoreWithTreatment());
    }

    @Test
    void testFallbackToMaxAggregatorWhenSchemaIsNull() {
        // Arrange
        String experimentName = "TestExperiment";
        String treatmentCode = "C";
        int treatmentCodeInInt = 1;
        List<String> modelsInExperiment = Arrays.asList("Model1", "Model2");

        ExperimentDefinition experimentDefinition = ExperimentDefinition.builder()
                .name(experimentName)
                .type("soft-filter")
                .aggregationSchema(null)  // null schema → fallback
                .build();

        when(mockContext.getExperimentContext()).thenReturn(mockExperimentContext);
        when(mockExperimentContext.getExperimentDefinitionByType("soft-filter")).thenReturn(experimentDefinition);
        when(mockExperimentContext.getModelsByExperiment()).thenReturn(Map.of(experimentName, modelsInExperiment));
        when(mockExperimentContext.getTreatmentCodeInInt(experimentName)).thenReturn(treatmentCodeInInt);
        when(mockExperimentContext.getTreatmentCode(experimentName)).thenReturn(treatmentCode);

        List<ModelEvaluatorOutput> outputs = Arrays.asList(
                createModelEvaluatorOutput("Model1", 0.5, ModelEvaluationStatus.SUCCESS),
                createModelEvaluatorOutput("Model2", 0.8, ModelEvaluationStatus.SUCCESS)
        );
        when(mockContext.getModelEvaluatorOutputs()).thenReturn(outputs);

        // Act
        AggregatedModelEvaluationResult result = aggregator.aggregate(mockContext);

        // Assert — should behave like MaxAggregator
        assertNotNull(result);
        assertEquals(experimentName, result.getExperimentName());
        assertEquals(0.8, result.getScore());
        assertEquals("max", result.getAggregationType());
    }

    @Test
    void testDefaultForwardForMissingModelResults() {
        // Arrange — schema references "Model1" and "ModelMissing", but only Model1 has output
        String experimentName = "TestExperiment";
        String treatmentCode = "C";
        int treatmentCodeInInt = 1;

        AggregationNode schema = createOperatorNode(AggregationOperator.AND, "Model1", "ModelMissing");

        ExperimentDefinition experimentDefinition = ExperimentDefinition.builder()
                .name(experimentName)
                .type("soft-filter")
                .aggregationSchema(schema)
                .build();

        when(mockContext.getExperimentContext()).thenReturn(mockExperimentContext);
        when(mockExperimentContext.getExperimentDefinitionByType("soft-filter")).thenReturn(experimentDefinition);
        when(mockExperimentContext.getTreatmentCodeInInt(experimentName)).thenReturn(treatmentCodeInInt);
        when(mockExperimentContext.getTreatmentCode(experimentName)).thenReturn(treatmentCode);

        List<ModelEvaluatorOutput> outputs = List.of(
                createModelEvaluatorOutput("Model1", 0.0, ModelEvaluationStatus.SUCCESS)
        );
        when(mockContext.getModelEvaluatorOutputs()).thenReturn(outputs);

        // Act
        AggregatedModelEvaluationResult result = aggregator.aggregate(mockContext);

        // Assert — AND(0.0, 1.0) = 1.0 because missing model defaults to 1.0
        assertNotNull(result);
        assertEquals(1.0, result.getScore());
    }

    @Test
    void testDefaultForwardForFailedModelResults() {
        // Arrange — Model2 has ERROR status, should not be in score map → defaults to 1.0
        String experimentName = "TestExperiment";
        String treatmentCode = "C";
        int treatmentCodeInInt = 1;

        AggregationNode schema = createOperatorNode(AggregationOperator.AND, "Model1", "Model2");

        ExperimentDefinition experimentDefinition = ExperimentDefinition.builder()
                .name(experimentName)
                .type("soft-filter")
                .aggregationSchema(schema)
                .build();

        when(mockContext.getExperimentContext()).thenReturn(mockExperimentContext);
        when(mockExperimentContext.getExperimentDefinitionByType("soft-filter")).thenReturn(experimentDefinition);
        when(mockExperimentContext.getTreatmentCodeInInt(experimentName)).thenReturn(treatmentCodeInInt);
        when(mockExperimentContext.getTreatmentCode(experimentName)).thenReturn(treatmentCode);

        List<ModelEvaluatorOutput> outputs = Arrays.asList(
                createModelEvaluatorOutput("Model1", 0.0, ModelEvaluationStatus.SUCCESS),
                createModelEvaluatorOutput("Model2", 0.0, ModelEvaluationStatus.ERROR)
        );
        when(mockContext.getModelEvaluatorOutputs()).thenReturn(outputs);

        // Act
        AggregatedModelEvaluationResult result = aggregator.aggregate(mockContext);

        // Assert — AND(0.0, 1.0) = 1.0 because failed model defaults to 1.0
        assertNotNull(result);
        assertEquals(1.0, result.getScore());
    }

    @Test
    void testExceptionDuringAggregationReturnsFallback() {
        // Arrange
        when(mockContext.getExperimentContext()).thenThrow(new RuntimeException("Test exception"));

        // Act
        AggregatedModelEvaluationResult result = aggregator.aggregate(mockContext);

        // Assert
        assertNotNull(result);
        assertEquals("UnknownExperiment", result.getExperimentName());
        assertEquals(1.0, result.getScore());
        assertEquals(1.0, result.getScoreWithTreatment());
        assertEquals("configurable", result.getAggregationType());
        verify(mockContext).addError(anyString());
    }

    @Test
    void testExperimentMetadataPreserved() {
        // Arrange
        String experimentName = "MySoftFilterExperiment";

        AggregationNode schema = createOperatorNode(AggregationOperator.OR, "Model1");

        ExperimentDefinition experimentDefinition = ExperimentDefinition.builder()
                .name(experimentName)
                .type("soft-filter")
                .aggregationSchema(schema)
                .build();

        when(mockContext.getExperimentContext()).thenReturn(mockExperimentContext);
        when(mockExperimentContext.getExperimentDefinitionByType("soft-filter")).thenReturn(experimentDefinition);
        when(mockExperimentContext.getTreatmentCodeInInt(experimentName)).thenReturn(1);
        when(mockExperimentContext.getTreatmentCode(experimentName)).thenReturn("C");

        List<ModelEvaluatorOutput> outputs = List.of(
                createModelEvaluatorOutput("Model1", 1.0, ModelEvaluationStatus.SUCCESS)
        );
        when(mockContext.getModelEvaluatorOutputs()).thenReturn(outputs);

        // Act
        AggregatedModelEvaluationResult result = aggregator.aggregate(mockContext);

        // Assert
        assertNotNull(result);
        assertEquals(experimentName, result.getExperimentName());
        assertEquals("soft-filter", result.getExperimentType());
        assertEquals("configurable", result.getAggregationType());
    }

    // Helper methods

    private static AggregationNode createLeaf(String modelIdentifier) {
        AggregationNode leaf = new AggregationNode();
        leaf.setModelIdentifier(modelIdentifier);
        return leaf;
    }

    private static AggregationNode createOperatorNode(AggregationOperator operator, String... modelIds) {
        AggregationNode node = new AggregationNode();
        node.setOperator(operator);
        node.setConditions(Arrays.stream(modelIds)
                .map(ConfigurableAggregatorTest::createLeaf)
                .collect(java.util.stream.Collectors.toList()));
        return node;
    }

    private ModelEvaluatorOutput createModelEvaluatorOutput(String modelId, double value, ModelEvaluationStatus status) {
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
