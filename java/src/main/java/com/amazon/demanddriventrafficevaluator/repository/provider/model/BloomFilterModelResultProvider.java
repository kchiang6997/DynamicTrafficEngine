// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.repository.provider.model;

import com.amazon.demanddriventrafficevaluator.modelfeature.ModelFeature;
import com.amazon.demanddriventrafficevaluator.repository.dao.Dao;
import com.amazon.demanddriventrafficevaluator.repository.entity.ModelResult;
import com.google.common.hash.BloomFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import lombok.extern.log4j.Log4j2;

/**
 * A provider class for retrieving bloom filter-based model results.
 * <p>
 * This class implements the ModelResultProvider interface and is responsible for
 * providing model results by checking feature tuple permutations against a Guava
 * BloomFilter. It uses the same key permutation logic as
 * {@link RuleBasedModelResultProvider} but performs probabilistic set membership
 * lookups instead of exact-match cache lookups.
 * </p>
 * <p>
 * Scoring follows the same pattern as RuleBasedModelResultProvider:
 * <ul>
 *   <li>On bloom filter hit (mightContain == true): score = ModelValueType.getCacheValue()</li>
 *   <li>On bloom filter miss (mightContain == false): score = ModelValueType.getDefaultValue()</li>
 * </ul>
 * If no bloom filter is found for the model identifier, the default value from
 * ModelValueType is returned (1.0 for LowValue = forward).
 * </p>
 */
@Log4j2
public class BloomFilterModelResultProvider implements ModelResultProvider {

    private static final String KEY_DELIMITER = "|";

    private final Dao<String, BloomFilter<String>> bloomFilterDao;

    public BloomFilterModelResultProvider(Dao<String, BloomFilter<String>> bloomFilterDao) {
        this.bloomFilterDao = bloomFilterDao;
    }

    /**
     * Provides a ModelResult based on the input features and model definition.
     * <p>
     * This method builds keys from the input features, checks each key against the
     * bloom filter, and constructs a ModelResult. On the first bloom filter hit,
     * the overall score is set to the model's cache value. If no hits are found,
     * the default value from ModelValueType is used.
     * </p>
     * <p>
     * If no bloom filter is loaded for the model identifier, the default value
     * is returned (1.0 for LowValue = forward by default).
     * </p>
     *
     * @param input The ModelResultProviderInput containing model features and definition.
     * @return A ModelResult containing the keys, per-key values, and overall value.
     */
    @Override
    public ModelResult provide(ModelResultProviderInput input) {
        List<String> keys = buildKeys(input.getModelFeatures());
        log.debug("In BloomFilterModelResultProvider keys: {}", keys);

        String modelIdentifier = input.getModelDefinition().getIdentifier();
        double cacheValue = input.getModelDefinition().getType().getCacheValue();
        double defaultValue = input.getModelDefinition().getType().getDefaultValue();

        Optional<BloomFilter<String>> bloomFilterOpt = bloomFilterDao.get(modelIdentifier, modelIdentifier);

        if (bloomFilterOpt.isEmpty()) {
            log.debug("No bloom filter found for model identifier: {}, returning default value: {}",
                    modelIdentifier, defaultValue);
            List<Double> values = new ArrayList<>(keys.size());
            for (int i = 0; i < keys.size(); i++) {
                values.add(defaultValue);
            }
            return ModelResult.builder()
                    .keys(keys)
                    .values(values)
                    .value(defaultValue)
                    .build();
        }

        BloomFilter<String> bloomFilter = bloomFilterOpt.get();
        List<Double> values = new ArrayList<>(keys.size());
        double value = defaultValue;
        boolean hit = false;

        for (String key : keys) {
            if (bloomFilter.mightContain(key)) {
                values.add(cacheValue);
                if (!hit) {
                    hit = true;
                    value = cacheValue;
                }
            } else {
                values.add(defaultValue);
            }
        }

        log.debug("In BloomFilterModelResultProvider values: {}", values);
        return ModelResult.builder()
                .keys(keys)
                .values(values)
                .value(value)
                .build();
    }

    /**
     * Builds every key permutation from the input features' values, with "|" delimiter.
     * <p>
     * This uses the same permutation logic as {@link RuleBasedModelResultProvider#buildKeys(List)}.
     * </p>
     *
     * @param modelFeatures the list of model features derived from the OpenRTB request
     * @return list of all tuple permutations of the input features' values
     */
    List<String> buildKeys(List<ModelFeature> modelFeatures) {
        if (modelFeatures == null || modelFeatures.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<String> result = new ArrayList<>(modelFeatures.get(0).getValues());

            for (int i = 1; i < modelFeatures.size(); i++) {
                result = addFeatureToPermutations(modelFeatures, i, result);
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to build key tuples", e);
            return Collections.emptyList();
        }
    }

    private List<String> addFeatureToPermutations(List<ModelFeature> modelFeatures, int i, List<String> result) {
        List<String> currentFeatureValues = modelFeatures.get(i).getValues();
        List<String> newPermutations = new ArrayList<>(result.size() * currentFeatureValues.size());

        for (String existingKey : result) {
            for (String newValue : currentFeatureValues) {
                newPermutations.add(existingKey + KEY_DELIMITER + newValue);
            }
        }
        return newPermutations;
    }
}
