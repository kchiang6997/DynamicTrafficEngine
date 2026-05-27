// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.repository.dao;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.BloomFilter;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A Data Access Object (DAO) implementation for storing and retrieving
 * Guava {@link BloomFilter} instances in memory with time-based eviction.
 * <p>
 * Each bloom filter is keyed by a model identifier. The {@code key} parameter
 * in {@link #get(String, String)} is ignored because each model has exactly
 * one bloom filter — the identifier is the lookup key.
 * </p>
 * <p>
 * Uses a Guava {@link Cache} with expireAfterAccess eviction. Entries that are
 * not accessed (read or written) within the TTL are automatically evicted,
 * preventing memory leaks from models that are removed from configuration.
 * Since bloom filters are read on every bid request evaluation, active models
 * stay alive indefinitely. Only models removed from config (no longer queried)
 * get evicted.
 * </p>
 */
public final class BloomFilterDao implements Dao<String, BloomFilter<String>> {

    private static final long DEFAULT_TTL_MINUTES = 60;

    private final Cache<String, BloomFilter<String>> store;

    public BloomFilterDao() {
        this(DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public BloomFilterDao(long ttl, TimeUnit timeUnit) {
        this.store = CacheBuilder.newBuilder()
                .expireAfterAccess(ttl, timeUnit)
                .build();
    }

    /**
     * Retrieves the bloom filter associated with the given identifier.
     * <p>
     * The {@code key} parameter is ignored — the identifier alone determines
     * which bloom filter is returned.
     * </p>
     *
     * @param identifier The model identifier to look up.
     * @param key        Ignored. Present to satisfy the {@link Dao} interface contract.
     * @return An Optional containing the bloom filter if found, or empty if not.
     */
    @Override
    public Optional<BloomFilter<String>> get(String identifier, String key) {
        return Optional.ofNullable(store.getIfPresent(identifier));
    }

    /**
     * Stores a bloom filter for the given identifier, replacing any existing one.
     * The TTL is reset on each put.
     * <p>
     * The {@code key} parameter is ignored — the identifier alone determines
     * where the bloom filter is stored.
     * </p>
     *
     * @param identifier The model identifier to associate with the bloom filter.
     * @param key        Ignored. Present to satisfy the {@link Dao} interface contract.
     * @param value      The bloom filter instance to store.
     */
    @Override
    public void put(String identifier, String key, BloomFilter<String> value) {
        store.put(identifier, value);
    }

    /**
     * Removes the bloom filter associated with the given identifier.
     *
     * @param identifier The model identifier whose bloom filter should be removed.
     */
    @Override
    public void clear(String identifier) {
        store.invalidate(identifier);
    }
}
