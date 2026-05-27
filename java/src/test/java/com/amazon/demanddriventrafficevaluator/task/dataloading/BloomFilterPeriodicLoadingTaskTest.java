// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.task.dataloading;

import com.amazon.demanddriventrafficevaluator.repository.entity.ModelConfiguration;
import com.amazon.demanddriventrafficevaluator.repository.entity.ModelDefinition;
import com.amazon.demanddriventrafficevaluator.repository.entity.ModelFormat;
import com.amazon.demanddriventrafficevaluator.repository.loader.DefaultLoader;
import com.amazon.demanddriventrafficevaluator.repository.loader.model.ModelResultLoaderInput;
import com.amazon.demanddriventrafficevaluator.repository.provider.configuration.ConfigurationProvider;
import com.amazon.demanddriventrafficevaluator.util.PropertiesUtil;
import org.apache.commons.configuration2.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloomFilterPeriodicLoadingTaskTest {

    @Mock
    private ScheduledThreadPoolExecutor mockExecutor;

    @Mock
    private ConfigurationProvider<ModelConfiguration> mockModelConfigurationProvider;

    @Mock
    private DefaultLoader<ModelResultLoaderInput> mockBloomFilterModelLoader;

    @Mock
    private ModelConfiguration mockModelConfiguration;

    @Mock
    private Configuration mockFileSharingS3BucketProperties;

    private BloomFilterPeriodicLoadingTask task;

    @BeforeEach
    void setUp() {
        task = new BloomFilterPeriodicLoadingTask(
                "testSSP",
                "BloomFilterLoadingTask",
                5000,
                mockExecutor,
                mockModelConfigurationProvider,
                mockBloomFilterModelLoader,
                "test-bucket"
        );
    }

    @Test
    void testExecuteTask_onlyBloomFilterModelsAreLoaded() {
        try (MockedStatic<PropertiesUtil> mockPropertiesUtil = mockStatic(PropertiesUtil.class)) {
            mockPropertiesUtil.when(PropertiesUtil::getFileSharingS3BucketProperties)
                    .thenReturn(mockFileSharingS3BucketProperties);

            // Arrange
            Map<String, ModelDefinition> modelDefinitions = new HashMap<>();

            ModelDefinition bloomFilterModel = new ModelDefinition();
            bloomFilterModel.setIdentifier("bf_model_1");
            bloomFilterModel.setModelFormat(ModelFormat.BLOOM_FILTER);
            modelDefinitions.put("bf_model_1", bloomFilterModel);

            ModelDefinition bloomFilterModel2 = new ModelDefinition();
            bloomFilterModel2.setIdentifier("bf_model_2");
            bloomFilterModel2.setModelFormat(ModelFormat.BLOOM_FILTER);
            modelDefinitions.put("bf_model_2", bloomFilterModel2);

            when(mockModelConfigurationProvider.provide()).thenReturn(mockModelConfiguration);
            when(mockModelConfiguration.getModelDefinitionByIdentifier()).thenReturn(modelDefinitions);
            when(mockFileSharingS3BucketProperties.getString("adsp", "test-bucket"))
                    .thenReturn("testBucket");

            // Act
            task.executeTask();

            // Assert
            ArgumentCaptor<ModelResultLoaderInput> captor = ArgumentCaptor.forClass(ModelResultLoaderInput.class);
            verify(mockBloomFilterModelLoader, times(2)).load(captor.capture());

            List<ModelResultLoaderInput> inputs = captor.getAllValues();
            assertEquals(2, inputs.size());
            assertTrue(inputs.stream().anyMatch(i -> i.getModelIdentifier().equals("bf_model_1")));
            assertTrue(inputs.stream().anyMatch(i -> i.getModelIdentifier().equals("bf_model_2")));
        }
    }

    @Test
    void testExecuteTask_ruleBasedModelsAreSkipped() {
        try (MockedStatic<PropertiesUtil> mockPropertiesUtil = mockStatic(PropertiesUtil.class)) {
            mockPropertiesUtil.when(PropertiesUtil::getFileSharingS3BucketProperties)
                    .thenReturn(mockFileSharingS3BucketProperties);

            // Arrange
            Map<String, ModelDefinition> modelDefinitions = new HashMap<>();

            ModelDefinition ruleBasedModel = new ModelDefinition();
            ruleBasedModel.setIdentifier("rule_model_1");
            ruleBasedModel.setModelFormat(ModelFormat.RULE_BASED);
            modelDefinitions.put("rule_model_1", ruleBasedModel);

            ModelDefinition bloomFilterModel = new ModelDefinition();
            bloomFilterModel.setIdentifier("bf_model_1");
            bloomFilterModel.setModelFormat(ModelFormat.BLOOM_FILTER);
            modelDefinitions.put("bf_model_1", bloomFilterModel);

            when(mockModelConfigurationProvider.provide()).thenReturn(mockModelConfiguration);
            when(mockModelConfiguration.getModelDefinitionByIdentifier()).thenReturn(modelDefinitions);
            when(mockFileSharingS3BucketProperties.getString("adsp", "test-bucket"))
                    .thenReturn("testBucket");

            // Act
            task.executeTask();

            // Assert
            ArgumentCaptor<ModelResultLoaderInput> captor = ArgumentCaptor.forClass(ModelResultLoaderInput.class);
            verify(mockBloomFilterModelLoader, times(1)).load(captor.capture());

            ModelResultLoaderInput input = captor.getValue();
            assertEquals("bf_model_1", input.getModelIdentifier());
        }
    }

    @Test
    void testExecuteTask_individualModelLoadFailureDoesNotInterruptRefreshCycle() {
        try (MockedStatic<PropertiesUtil> mockPropertiesUtil = mockStatic(PropertiesUtil.class)) {
            mockPropertiesUtil.when(PropertiesUtil::getFileSharingS3BucketProperties)
                    .thenReturn(mockFileSharingS3BucketProperties);

            // Arrange
            Map<String, ModelDefinition> modelDefinitions = new HashMap<>();

            ModelDefinition bloomFilterModel1 = new ModelDefinition();
            bloomFilterModel1.setIdentifier("bf_model_1");
            bloomFilterModel1.setModelFormat(ModelFormat.BLOOM_FILTER);
            modelDefinitions.put("bf_model_1", bloomFilterModel1);

            ModelDefinition bloomFilterModel2 = new ModelDefinition();
            bloomFilterModel2.setIdentifier("bf_model_2");
            bloomFilterModel2.setModelFormat(ModelFormat.BLOOM_FILTER);
            modelDefinitions.put("bf_model_2", bloomFilterModel2);

            when(mockModelConfigurationProvider.provide()).thenReturn(mockModelConfiguration);
            when(mockModelConfiguration.getModelDefinitionByIdentifier()).thenReturn(modelDefinitions);
            when(mockFileSharingS3BucketProperties.getString("adsp", "test-bucket"))
                    .thenReturn("testBucket");

            // First call throws, second call succeeds
            when(mockBloomFilterModelLoader.load(any(ModelResultLoaderInput.class)))
                    .thenThrow(new RuntimeException("S3 failure"))
                    .thenReturn(true);

            // Act
            task.executeTask();

            // Assert - both models were attempted despite the first one failing
            verify(mockBloomFilterModelLoader, times(2)).load(any(ModelResultLoaderInput.class));
        }
    }

    @Test
    void testExecuteTask_withEmptyModelDefinitions() {
        try (MockedStatic<PropertiesUtil> mockPropertiesUtil = mockStatic(PropertiesUtil.class)) {
            mockPropertiesUtil.when(PropertiesUtil::getFileSharingS3BucketProperties)
                    .thenReturn(mockFileSharingS3BucketProperties);

            // Arrange
            when(mockModelConfigurationProvider.provide()).thenReturn(mockModelConfiguration);
            when(mockModelConfiguration.getModelDefinitionByIdentifier()).thenReturn(new HashMap<>());
            when(mockFileSharingS3BucketProperties.getString("adsp", "test-bucket"))
                    .thenReturn("testBucket");

            // Act
            task.executeTask();

            // Assert
            verify(mockBloomFilterModelLoader, never()).load(any());
        }
    }

    @Test
    void testInitialize() {
        // Act
        task.initialize();

        // Assert
        verify(mockExecutor).scheduleAtFixedRate(any(Runnable.class), anyLong(), eq(5000L), eq(TimeUnit.MILLISECONDS));
    }
}
