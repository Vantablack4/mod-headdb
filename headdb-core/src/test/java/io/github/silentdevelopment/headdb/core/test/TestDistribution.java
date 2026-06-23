package io.github.silentdevelopment.headdb.core.test;

import com.github.luben.zstd.Zstd;
import io.github.silentdevelopment.headdb.core.hash.Sha256Verifier;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TestDistribution {

    public static final URI MANIFEST_URI = URI.create("https://data.headsdb.com/manifest.json");
    public static final URI CATALOG_INDEX_URI = URI.create("https://mirror.headsdb.com/artifacts/catalog/index-test.json.zst");
    public static final URI CATALOG_PART_URI = URI.create("https://mirror.headsdb.com/artifacts/catalog/catalog-part-0000-test.json.zst");
    public static final URI CATALOG_PART_1_URI = URI.create("https://mirror.headsdb.com/artifacts/catalog/catalog-part-0001-test.json.zst");
    public static final URI REVOCATIONS_INDEX_URI = URI.create("https://mirror.headsdb.com/artifacts/revocations/index-test.json.zst");
    public static final URI REVOCATIONS_PART_URI = URI.create("https://mirror.headsdb.com/artifacts/revocations/revocations-part-0000-test.json.zst");
    public static final URI REVOCATIONS_PART_1_URI = URI.create("https://mirror.headsdb.com/artifacts/revocations/revocations-part-0001-test.json.zst");

    private static final Sha256Verifier SHA256 = new Sha256Verifier();

    public final String manifestJson;
    public final byte[] catalogIndex;
    public final byte[] catalogPart;
    public final byte[] revocationsIndex;
    public final byte[] revocationsPart;
    public final Map<URI, byte[]> artifacts;

    private TestDistribution(String manifestJson, byte[] catalogIndex, byte[] catalogPart, byte[] revocationsIndex, byte[] revocationsPart, Map<URI, byte[]> artifacts) {
        this.manifestJson = manifestJson;
        this.catalogIndex = catalogIndex;
        this.catalogPart = catalogPart;
        this.revocationsIndex = revocationsIndex;
        this.revocationsPart = revocationsPart;
        this.artifacts = Map.copyOf(artifacts);
    }

    public static TestDistribution create() {
        byte[] catalogPart = compressed(catalogPartJson());
        byte[] revocationsPart = compressed(revocationsPartJson());
        byte[] catalogIndex = compressed(catalogIndexJson(catalogPart));
        byte[] revocationsIndex = compressed(revocationsIndexJson(revocationsPart));
        String manifestJson = manifestJson(catalogIndex, revocationsIndex);
        Map<URI, byte[]> artifacts = new LinkedHashMap<>();
        artifacts.put(CATALOG_INDEX_URI, catalogIndex);
        artifacts.put(CATALOG_PART_URI, catalogPart);
        artifacts.put(REVOCATIONS_INDEX_URI, revocationsIndex);
        artifacts.put(REVOCATIONS_PART_URI, revocationsPart);
        return new TestDistribution(manifestJson, catalogIndex, catalogPart, revocationsIndex, revocationsPart, artifacts);
    }

    public static TestDistribution createMultiPart() {
        byte[] catalogPart0 = compressed(catalogPartZeroJson());
        byte[] catalogPart1 = compressed(catalogPartOneJson());
        byte[] revocationsPart0 = compressed(revocationsPartJson());
        byte[] revocationsPart1 = compressed(emptyRevocationsPartJson());
        byte[] catalogIndex = compressed(catalogIndexJson(catalogPart0, catalogPart1));
        byte[] revocationsIndex = compressed(revocationsIndexJson(revocationsPart0, revocationsPart1));
        String manifestJson = manifestJson(catalogIndex, revocationsIndex);
        Map<URI, byte[]> artifacts = new LinkedHashMap<>();
        artifacts.put(CATALOG_INDEX_URI, catalogIndex);
        artifacts.put(CATALOG_PART_URI, catalogPart0);
        artifacts.put(CATALOG_PART_1_URI, catalogPart1);
        artifacts.put(REVOCATIONS_INDEX_URI, revocationsIndex);
        artifacts.put(REVOCATIONS_PART_URI, revocationsPart0);
        artifacts.put(REVOCATIONS_PART_1_URI, revocationsPart1);
        return new TestDistribution(manifestJson, catalogIndex, catalogPart0, revocationsIndex, revocationsPart0, artifacts);
    }

    public static byte[] compressed(String json) {
        return Zstd.compress(json.getBytes(StandardCharsets.UTF_8));
    }

    public static String catalogIndexJson(byte[] catalogPart) {
        return """
            {
              "schema": 1,
              "id": "catalog",
              "categories": [
                {"id":"animals","name":"Animals","description":"Animal heads"},
                {"id":"blocks","name":"Blocks","description":"Block heads"}
              ],
              "tags": [
                {"id":"cute","name":"Cute","description":"Cute heads"},
                {"id":"fire","name":"Fire","description":"Fire heads"}
              ],
              "collections": [
                {"id":"christmas","name":"Christmas","description":"Christmas heads"}
              ],
              "artifacts": [
                %s
              ]
            }
            """.formatted(catalogPartDescriptor(0, CATALOG_PART_URI, catalogPart));
    }

    public static String catalogIndexJson(byte[] catalogPart0, byte[] catalogPart1) {
        return """
            {
              "schema": 1,
              "id": "catalog",
              "categories": [
                {"id":"animals","name":"Animals","description":"Animal heads"},
                {"id":"blocks","name":"Blocks","description":"Block heads"}
              ],
              "tags": [
                {"id":"cute","name":"Cute","description":"Cute heads"},
                {"id":"fire","name":"Fire","description":"Fire heads"}
              ],
              "collections": [
                {"id":"christmas","name":"Christmas","description":"Christmas heads"}
              ],
              "artifacts": [
                %s,
                %s
              ]
            }
            """.formatted(catalogPartDescriptor(0, CATALOG_PART_URI, catalogPart0), catalogPartDescriptor(1, CATALOG_PART_1_URI, catalogPart1));
    }

    public static String revocationsIndexJson(byte[] revocationsPart) {
        return """
            {
              "schema": 1,
              "id": "revocations",
              "artifacts": [
                %s
              ]
            }
            """.formatted(revocationsPartDescriptor(0, REVOCATIONS_PART_URI, revocationsPart));
    }

    public static String revocationsIndexJson(byte[] revocationsPart0, byte[] revocationsPart1) {
        return """
            {
              "schema": 1,
              "id": "revocations",
              "artifacts": [
                %s,
                %s
              ]
            }
            """.formatted(revocationsPartDescriptor(0, REVOCATIONS_PART_URI, revocationsPart0), revocationsPartDescriptor(1, REVOCATIONS_PART_1_URI, revocationsPart1));
    }

    public static String manifestJson(byte[] catalogIndex, byte[] revocationsIndex) {
        return """
            {
              "schema": 1,
              "id": "heads",
              "revision": 1,
              "timestamp": "2026-01-01T00:00:00Z",
              "mirrors": [
                {"id":"primary","priority":0,"url":"https://mirror.headsdb.com/"}
              ],
              "resources": {
                "catalog": {
                  "index": {
                    "path": "artifacts/catalog/index-test.json.zst",
                    "contentType": "application/vnd.heads.catalog-index+json",
                    "compression": "zstd",
                    "integrity": {"algorithm":"sha256","digest":"%s","bytes":%d}
                  }
                },
                "revocations": {
                  "index": {
                    "path": "artifacts/revocations/index-test.json.zst",
                    "contentType": "application/vnd.heads.revocations-index+json",
                    "compression": "zstd",
                    "integrity": {"algorithm":"sha256","digest":"%s","bytes":%d}
                  }
                }
              }
            }
            """.formatted(SHA256.hash(catalogIndex), catalogIndex.length, SHA256.hash(revocationsIndex), revocationsIndex.length);
    }

    private static String catalogPartDescriptor(int index, URI uri, byte[] bytes) {
        return """
                {
                  "index": %d,
                  "id": "catalog-part-%04d",
                  "path": "%s",
                  "contentType": "application/vnd.heads.catalog-part+json",
                  "compression": "zstd",
                  "integrity": {"algorithm":"sha256","digest":"%s","bytes":%d}
                }""".formatted(index, index, path(uri), SHA256.hash(bytes), bytes.length);
    }

    private static String revocationsPartDescriptor(int index, URI uri, byte[] bytes) {
        return """
                {
                  "index": %d,
                  "id": "revocations-part-%04d",
                  "path": "%s",
                  "contentType": "application/vnd.heads.revocations-part+json",
                  "compression": "zstd",
                  "integrity": {"algorithm":"sha256","digest":"%s","bytes":%d}
                }""".formatted(index, index, path(uri), SHA256.hash(bytes), bytes.length);
    }

    private static String path(URI uri) {
        String path = uri.getPath();
        if (path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }

    private static String catalogPartJson() {
        return """
            {
              "schema": 1,
              "heads": [
                {"id":1,"name":"Cute Cat","texture":"texture1","category":"animals","tags":["cute"],"collections":["christmas"]},
                {"id":2,"name":"Revoked Dragon","texture":"texture2","category":"blocks","tags":["fire"],"collections":[]},
                {"id":3,"name":"Plain Block","texture":"texture3","category":"blocks","tags":[],"collections":[]}
              ]
            }
            """;
    }

    private static String catalogPartZeroJson() {
        return """
            {
              "schema": 1,
              "heads": [
                {"id":1,"name":"Cute Cat","texture":"texture1","category":"animals","tags":["cute"],"collections":["christmas"]}
              ]
            }
            """;
    }

    private static String catalogPartOneJson() {
        return """
            {
              "schema": 1,
              "heads": [
                {"id":2,"name":"Revoked Dragon","texture":"texture2","category":"blocks","tags":["fire"],"collections":[]},
                {"id":3,"name":"Plain Block","texture":"texture3","category":"blocks","tags":[],"collections":[]}
              ]
            }
            """;
    }

    private static String revocationsPartJson() {
        return """
            {
              "schema": 1,
              "revocations": [
                {"id":2,"reason":"invalid texture","revokedAt":"2026-01-01T00:00:00Z"}
              ]
            }
            """;
    }

    private static String emptyRevocationsPartJson() {
        return """
            {
              "schema": 1,
              "revocations": []
            }
            """;
    }
}
