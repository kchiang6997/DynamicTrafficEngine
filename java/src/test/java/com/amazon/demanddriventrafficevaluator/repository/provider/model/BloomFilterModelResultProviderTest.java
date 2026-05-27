// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.repository.provider.model;

import com.amazon.demanddriventrafficevaluator.modelfeature.ModelFeature;
import com.amazon.demanddriventrafficevaluator.repository.dao.Dao;
import com.amazon.demanddriventrafficevaluator.repository.entity.ModelDefinition;
import com.amazon.demanddriventrafficevaluator.repository.entity.ModelResult;
import com.amazon.demanddriventrafficevaluator.repository.entity.ModelValueType;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloomFilterModelResultProviderTest {

    @Mock
    private Dao<String, BloomFilter<String>> mockBloomFilterDao;

    @Mock
    private ModelDefinition mockModelDefinition;

    private BloomFilterModelResultProvider provider;

    @BeforeEach
    void setUp() {
        provider = new BloomFilterModelResultProvider(mockBloomFilterDao);
    }

    @Test
    void provide_multiplePermutations_firstMatchDeterminesOverallValue() {
        // Arrange — bloom filter contains "A|2" but not "A|1"
        BloomFilter<String> bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 100, 0.01);
        bloomFilter.put("A|2");

        when(mockModelDefinition.getIdentifier()).thenReturn("model1");
        when(mockModelDefinition.getType()).thenReturn(ModelValueType.LowValue);
        when(mockBloomFilterDao.get("model1", "model1")).thenReturn(Optional.of(bloomFilter));

        List<ModelFeature> features = Arrays.asList(
                ModelFeature.builder().values(Collections.singletonList("A")).build(),
                ModelFeature.builder().values(Arrays.asList("1", "2")).build()
        );

        ModelResultProviderInput input = ModelResultProviderInput.builder()
                .modelDefinition(mockModelDefinition)
                .modelFeatures(features)
                .build();

        // Act
        ModelResult result = provider.provide(input);

        // Assert — keys are ["A|1", "A|2"], first match is "A|2" at index 1
        assertEquals(Arrays.asList("A|1", "A|2"), result.getKeys());
        // LowValue: cacheValue=0.0, defaultValue=1.0
        assertEquals(Arrays.asList(1.0, 0.0), result.getValues());
        // Overall value is the first hit: cacheValue = 0.0
        assertEquals(0.0, result.getValue());
    }

    @Test
    void provide_multiplePermutations_firstPermutationIsHit() {
        // Arrange — bloom filter contains "A|1" (the first permutation)
        BloomFilter<String> bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 100, 0.01);
        bloomFilter.put("A|1");

        when(mockModelDefinition.getIdentifier()).thenReturn("model1");
        when(mockModelDefinition.getType()).thenReturn(ModelValueType.LowValue);
        when(mockBloomFilterDao.get("model1", "model1")).thenReturn(Optional.of(bloomFilter));

        List<ModelFeature> features = Arrays.asList(
                ModelFeature.builder().values(Collections.singletonList("A")).build(),
                ModelFeature.builder().values(Arrays.asList("1", "2")).build()
        );

        ModelResultProviderInput input = ModelResultProviderInput.builder()
                .modelDefinition(mockModelDefinition)
                .modelFeatures(features)
                .build();

        // Act
        ModelResult result = provider.provide(input);

        // Assert
        assertEquals(Arrays.asList("A|1", "A|2"), result.getKeys());
        assertEquals(Arrays.asList(0.0, 1.0), result.getValues());
        // Overall value is the first hit: cacheValue = 0.0
        assertEquals(0.0, result.getValue());
    }

    @Test
    void provide_multiplePermutations_noMatch_returnsDefaultValue() {
        // Arrange — bloom filter is empty, no matches
        BloomFilter<String> bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 100, 0.01);

        when(mockModelDefinition.getIdentifier()).thenReturn("model1");
        when(mockModelDefinition.getType()).thenReturn(ModelValueType.LowValue);
        when(mockBloomFilterDao.get("model1", "model1")).thenReturn(Optional.of(bloomFilter));

        List<ModelFeature> features = Arrays.asList(
                ModelFeature.builder().values(Collections.singletonList("X")).build(),
                ModelFeature.builder().values(Arrays.asList("1", "2")).build()
        );

        ModelResultProviderInput input = ModelResultProviderInput.builder()
                .modelDefinition(mockModelDefinition)
                .modelFeatures(features)
                .build();

        // Act
        ModelResult result = provider.provide(input);

        // Assert
        assertEquals(Arrays.asList("X|1", "X|2"), result.getKeys());
        assertEquals(Arrays.asList(1.0, 1.0), result.getValues());
        // LowValue default = 1.0 (forward)
        assertEquals(1.0, result.getValue());
    }

    @Test
    void provide_noBloomFilterLoaded_returnsDefaultValue_LowValue() {
        // Arrange — no bloom filter in DAO
        when(mockModelDefinition.getIdentifier()).thenReturn("missing_model");
        when(mockModelDefinition.getType()).thenReturn(ModelValueType.LowValue);
        when(mockBloomFilterDao.get("missing_model", "missing_model")).thenReturn(Optional.empty());

        List<ModelFeature> features = Collections.singletonList(
                ModelFeature.builder().values(Collections.singletonList("tuple1")).build()
        );

        ModelResultProviderInput input = ModelResultProviderInput.builder()
                .modelDefinition(mockModelDefinition)
                .modelFeatures(features)
                .build();

        // Act
        ModelResult result = provider.provide(input);

        // Assert — LowValue default = 1.0 (forward)
        assertEquals(1.0, result.getValue());
        assertEquals(Collections.singletonList(1.0), result.getValues());
        assertEquals(Collections.singletonList("tuple1"), result.getKeys());
    }

    @Test
    void provide_noBloomFilterLoaded_returnsDefaultValue_HighValue() {
        // Arrange — no bloom filter in DAO
        when(mockModelDefinition.getIdentifier()).thenReturn("missing_model");
        when(mockModelDefinition.getType()).thenReturn(ModelValueType.HighValue);
        when(mockBloomFilterDao.get("missing_model", "missing_model")).thenReturn(Optional.empty());

        List<ModelFeature> features = Collections.singletonList(
                ModelFeature.builder().values(Collections.singletonList("tuple1")).build()
        );

        ModelResultProviderInput input = ModelResultProviderInput.builder()
                .modelDefinition(mockModelDefinition)
                .modelFeatures(features)
                .build();

        // Act
        ModelResult result = provider.provide(input);

        // Assert — HighValue default = 0.0
        assertEquals(0.0, result.getValue());
        assertEquals(Collections.singletonList(0.0), result.getValues());
    }

    @Test
    void provide_threeFeatures_multiplePermutations_firstMatchBehavior() {
        // Arrange — bloom filter contains "A|1|Y" but not the earlier permutations
        BloomFilter<String> bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 100, 0.01);
        bloomFilter.put("A|1|Y");

        when(mockModelDefinition.getIdentifier()).thenReturn("model1");
        when(mockModelDefinition.getType()).thenReturn(ModelValueType.LowValue);
        when(mockBloomFilterDao.get("model1", "model1")).thenReturn(Optional.of(bloomFilter));

        List<ModelFeature> features = Arrays.asList(
                ModelFeature.builder().values(Collections.singletonList("A")).build(),
                ModelFeature.builder().values(Arrays.asList("1", "2")).build(),
                ModelFeature.builder().values(Arrays.asList("X", "Y")).build()
        );

        ModelResultProviderInput input = ModelResultProviderInput.builder()
                .modelDefinition(mockModelDefinition)
                .modelFeatures(features)
                .build();

        // Act
        ModelResult result = provider.provide(input);

        // Assert — keys: [A|1|X, A|1|Y, A|2|X, A|2|Y]
        assertEquals(Arrays.asList("A|1|X", "A|1|Y", "A|2|X", "A|2|Y"), result.getKeys());
        // A|1|X=miss(1.0), A|1|Y=hit(0.0), A|2|X=miss(1.0), A|2|Y=miss(1.0)
        assertEquals(Arrays.asList(1.0, 0.0, 1.0, 1.0), result.getValues());
        // First hit is A|1|Y → cacheValue = 0.0
        assertEquals(0.0, result.getValue());
    }

    @Test
    void provide_emptyFeatures_returnsDefaultValue() {
        // Arrange
        when(mockModelDefinition.getIdentifier()).thenReturn("model1");
        when(mockModelDefinition.getType()).thenReturn(ModelValueType.LowValue);
        when(mockBloomFilterDao.get("model1", "model1")).thenReturn(Optional.empty());

        ModelResultProviderInput input = ModelResultProviderInput.builder()
                .modelDefinition(mockModelDefinition)
                .modelFeatures(Collections.emptyList())
                .build();

        // Act
        ModelResult result = provider.provide(input);

        // Assert
        assertTrue(result.getKeys().isEmpty());
        assertTrue(result.getValues().isEmpty());
        assertEquals(1.0, result.getValue());
    }

    @Test
    void buildKeys_emptyFeaturesList_returnsEmptyList() {
        List<String> result = provider.buildKeys(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    void buildKeys_nullFeatureList_returnsEmptyList() {
        List<String> result = provider.buildKeys(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void buildKeys_singleFeature_returnsFeatureValues() {
        ModelFeature feature = ModelFeature.builder()
                .values(Arrays.asList("A", "B"))
                .build();

        List<String> result = provider.buildKeys(Collections.singletonList(feature));

        assertEquals(Arrays.asList("A", "B"), result);
    }

    @Test
    void buildKeys_twoFeatures_returnsAllPermutations() {
        ModelFeature feature1 = ModelFeature.builder()
                .values(Arrays.asList("A", "B"))
                .build();
        ModelFeature feature2 = ModelFeature.builder()
                .values(Arrays.asList("1", "2"))
                .build();

        List<String> result = provider.buildKeys(Arrays.asList(feature1, feature2));

        assertEquals(Arrays.asList("A|1", "A|2", "B|1", "B|2"), result);
    }
}
