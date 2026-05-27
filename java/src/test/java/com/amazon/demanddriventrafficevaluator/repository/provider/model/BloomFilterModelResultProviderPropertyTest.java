// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.repository.provider.model;

import com.amazon.demanddriventrafficevaluator.modelfeature.ModelFeature;
import com.amazon.demanddriventrafficevaluator.repository.dao.BloomFilterDao;
import com.amazon.demanddriventrafficevaluator.repository.entity.ModelDefinition;
import com.amazon.demanddriventrafficevaluator.repository.entity.ModelResult;
import com.amazon.demanddriventrafficevaluator.repository.entity.ModelValueType;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Property test for bloom filter score truth table (Property 5).
 * <p>
 * <b>Validates: Requirements 3.2, 3.3</b>
 * <p>
 * For any feature tuple string, any Guava BloomFilter, and any ModelValueType configuration,
 * the score returned by BloomFilterModelResultProvider.provide() follows this truth table:
 * <ul>
 *   <li>mightContain(tuple) == true → score = ModelValueType.getCacheValue()</li>
 *   <li>mightContain(tuple) == false → score = ModelValueType.getDefaultValue()</li>
 * </ul>
 */
class BloomFilterModelResultProviderPropertyTest {

    private static final String MODEL_IDENTIFIER = "test_model_v1";
    private static final String HIT_TUPLE = "publisher123";
    private static final String MISS_TUPLE = "publisher_not_in_filter";

    /**
     * Property 5 — Hit case: mightContain == true → score = getCacheValue()
     *
     * **Validates: Requirements 3.2, 3.3**
     */
    @ParameterizedTest
    @EnumSource(ModelValueType.class)
    void provide_whenBloomFilterContainsTuple_returnsModelCacheValue(ModelValueType modelValueType) {
        // Arrange
        BloomFilterDao bloomFilterDao = new BloomFilterDao();
        BloomFilter<String> bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 100, 0.01);
        bloomFilter.put(HIT_TUPLE);
        bloomFilterDao.put(MODEL_IDENTIFIER, MODEL_IDENTIFIER, bloomFilter);

        BloomFilterModelResultProvider provider = new BloomFilterModelResultProvider(bloomFilterDao);

        ModelDefinition modelDefinition = new ModelDefinition();
        modelDefinition.setIdentifier(MODEL_IDENTIFIER);
        modelDefinition.setType(modelValueType);

        List<ModelFeature> features = Collections.singletonList(
                ModelFeature.builder().values(Collections.singletonList(HIT_TUPLE)).build()
        );

        ModelResultProviderInput input = ModelResultProviderInput.builder()
                .modelDefinition(modelDefinition)
                .modelFeatures(features)
                .build();

        // Act
        ModelResult result = provider.provide(input);

        // Assert
        assertEquals(modelValueType.getCacheValue(), result.getValue(),
                "Hit case: score should equal getCacheValue() for " + modelValueType);
        assertEquals(Collections.singletonList(modelValueType.getCacheValue()), result.getValues());
    }

    /**
     * Property 5 — Miss case: mightContain == false → score = getDefaultValue()
     *
     * **Validates: Requirements 3.2, 3.3**
     */
    @ParameterizedTest
    @EnumSource(ModelValueType.class)
    void provide_whenBloomFilterDoesNotContainTuple_returnsModelDefaultValue(ModelValueType modelValueType) {
        // Arrange
        BloomFilterDao bloomFilterDao = new BloomFilterDao();
        BloomFilter<String> bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 100, 0.01);
        // Do NOT put MISS_TUPLE into the bloom filter
        bloomFilterDao.put(MODEL_IDENTIFIER, MODEL_IDENTIFIER, bloomFilter);

        BloomFilterModelResultProvider provider = new BloomFilterModelResultProvider(bloomFilterDao);

        ModelDefinition modelDefinition = new ModelDefinition();
        modelDefinition.setIdentifier(MODEL_IDENTIFIER);
        modelDefinition.setType(modelValueType);

        List<ModelFeature> features = Collections.singletonList(
                ModelFeature.builder().values(Collections.singletonList(MISS_TUPLE)).build()
        );

        ModelResultProviderInput input = ModelResultProviderInput.builder()
                .modelDefinition(modelDefinition)
                .modelFeatures(features)
                .build();

        // Act
        ModelResult result = provider.provide(input);

        // Assert
        assertEquals(modelValueType.getDefaultValue(), result.getValue(),
                "Miss case: score should equal getDefaultValue() for " + modelValueType);
        assertEquals(Collections.singletonList(modelValueType.getDefaultValue()), result.getValues());
    }
}
