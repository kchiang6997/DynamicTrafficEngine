// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.demanddriventrafficevaluator.repository.dao;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloomFilterDaoTest {

    private BloomFilterDao bloomFilterDao;

    @BeforeEach
    void setUp() {
        bloomFilterDao = new BloomFilterDao();
    }

    // -----------------------------------------------------------------------
    // Property 3: BloomFilterDao put-get round trip
    // Validates: Requirements 2.2, 2.3
    // -----------------------------------------------------------------------

    static Stream<String> identifiersForPutGetRoundTrip() {
        return Stream.of(
                "adsp_inv_v1",
                "model-alpha",
                "vendor_low-value_v2",
                "single",
                "complex.identifier.with.dots"
        );
    }

    /**
     * **Validates: Requirements 2.2, 2.3**
     *
     * Property 3: For any model identifier and any BloomFilter instance,
     * storing via put(id, key, bf) and then calling get(id, key) returns
     * an Optional containing the same BloomFilter instance.
     */
    @ParameterizedTest
    @MethodSource("identifiersForPutGetRoundTrip")
    void putGetRoundTrip_returnsSameInstance(String identifier) {
        // Arrange
        BloomFilter<String> bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 100, 0.01);
        bloomFilter.put("test-element");

        // Act
        bloomFilterDao.put(identifier, "ignored-key", bloomFilter);
        Optional<BloomFilter<String>> result = bloomFilterDao.get(identifier, "any-key");

        // Assert
        assertTrue(result.isPresent());
        assertSame(bloomFilter, result.get());
    }

    @Test
    void putGetRoundTrip_multipleIdentifiers_eachReturnCorrectInstance() {
        // Arrange
        BloomFilter<String> bf1 = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 100, 0.01);
        BloomFilter<String> bf2 = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 200, 0.03);
        BloomFilter<String> bf3 = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 50, 0.05);

        // Act
        bloomFilterDao.put("model-a", "key", bf1);
        bloomFilterDao.put("model-b", "key", bf2);
        bloomFilterDao.put("model-c", "key", bf3);

        // Assert
        assertSame(bf1, bloomFilterDao.get("model-a", "key").orElse(null));
        assertSame(bf2, bloomFilterDao.get("model-b", "key").orElse(null));
        assertSame(bf3, bloomFilterDao.get("model-c", "key").orElse(null));
    }

    @Test
    void putGetRoundTrip_keyParameterIsIgnored() {
        // Arrange
        BloomFilter<String> bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 100, 0.01);

        // Act
        bloomFilterDao.put("model-id", "original-key", bloomFilter);

        // Assert — different keys all return the same bloom filter
        assertSame(bloomFilter, bloomFilterDao.get("model-id", "different-key").orElse(null));
        assertSame(bloomFilter, bloomFilterDao.get("model-id", "another-key").orElse(null));
        assertSame(bloomFilter, bloomFilterDao.get("model-id", null).orElse(null));
    }

    @Test
    void put_replacesExistingBloomFilter() {
        // Arrange
        BloomFilter<String> original = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 100, 0.01);
        BloomFilter<String> replacement = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 200, 0.01);

        // Act
        bloomFilterDao.put("model-id", "key", original);
        bloomFilterDao.put("model-id", "key", replacement);
        Optional<BloomFilter<String>> result = bloomFilterDao.get("model-id", "key");

        // Assert
        assertTrue(result.isPresent());
        assertSame(replacement, result.get());
    }

    // -----------------------------------------------------------------------
    // Property 4: BloomFilterDao empty on missing or cleared identifier
    // Validates: Requirements 2.4, 2.5
    // -----------------------------------------------------------------------

    static Stream<String> identifiersForMissingLookup() {
        return Stream.of(
                "never-stored-model",
                "nonexistent",
                "adsp_inv_v99",
                "",
                "model.with.dots"
        );
    }

    /**
     * **Validates: Requirements 2.4, 2.5**
     *
     * Property 4: For any model identifier that has never been stored,
     * calling get(id, key) returns an empty Optional.
     */
    @ParameterizedTest
    @MethodSource("identifiersForMissingLookup")
    void get_neverStoredIdentifier_returnsEmpty(String identifier) {
        // Act
        Optional<BloomFilter<String>> result = bloomFilterDao.get(identifier, "any-key");

        // Assert
        assertFalse(result.isPresent());
    }

    static Stream<String> identifiersForClearTest() {
        return Stream.of(
                "adsp_inv_v1",
                "model-to-clear",
                "vendor_low-value_v2"
        );
    }

    /**
     * **Validates: Requirements 2.4, 2.5**
     *
     * Property 4: For any model identifier that has been stored and then cleared,
     * calling get(id, key) returns an empty Optional.
     */
    @ParameterizedTest
    @MethodSource("identifiersForClearTest")
    void get_afterClear_returnsEmpty(String identifier) {
        // Arrange
        BloomFilter<String> bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 100, 0.01);
        bloomFilterDao.put(identifier, "key", bloomFilter);

        // Verify it was stored
        assertTrue(bloomFilterDao.get(identifier, "key").isPresent());

        // Act
        bloomFilterDao.clear(identifier);

        // Assert
        assertFalse(bloomFilterDao.get(identifier, "key").isPresent());
    }

    @Test
    void clear_doesNotAffectOtherIdentifiers() {
        // Arrange
        BloomFilter<String> bf1 = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 100, 0.01);
        BloomFilter<String> bf2 = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), 100, 0.01);
        bloomFilterDao.put("model-a", "key", bf1);
        bloomFilterDao.put("model-b", "key", bf2);

        // Act
        bloomFilterDao.clear("model-a");

        // Assert
        assertFalse(bloomFilterDao.get("model-a", "key").isPresent());
        assertTrue(bloomFilterDao.get("model-b", "key").isPresent());
        assertSame(bf2, bloomFilterDao.get("model-b", "key").get());
    }

    @Test
    void clear_nonExistentIdentifier_doesNotThrow() {
        // Act & Assert — should not throw
        bloomFilterDao.clear("nonexistent-model");
    }
}
