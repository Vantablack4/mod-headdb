package io.github.silentdevelopment.headdb.core.remote.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.silentdevelopment.headdb.core.remote.RemoteContentTypes;
import io.github.silentdevelopment.headdb.core.remote.RemoteManifest;
import io.github.silentdevelopment.headdb.core.remote.RemoteResourceId;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;

final class GsonRemoteManifestParserTest {

    private final GsonRemoteManifestParser parser = new GsonRemoteManifestParser();

    @Test
    void parsesValidManifest() {
        RemoteManifest manifest = parser.parse(validManifestJson());
        assertEquals(1, manifest.schema());
        assertEquals("heads", manifest.id());
        assertEquals(1, manifest.revision());
        assertEquals(1, manifest.mirrors().size());
        assertEquals(2, manifest.resources().size());
        assertEquals(RemoteContentTypes.CATALOG_INDEX, manifest.catalogResource().index().contentType());
        assertEquals(RemoteContentTypes.REVOCATIONS_INDEX, manifest.revocationsResource().index().contentType());
    }

    @Test
    void rejectsEmptyJson() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("   "));
    }

    @Test
    void rejectsNullJson() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }

    @Test
    void rejectsInvalidJsonSyntax() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("{"));
    }

    @Test
    void rejectsJsonNull() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("null"));
    }

    @Test
    void rejectsInvalidTimestamp() {
        String json = validManifestJson().replace("2026-01-01T00:00:00Z", "not-an-instant");
        assertThrows(DateTimeParseException.class, () -> parser.parse(json));
    }

    @Test
    void rejectsMissingRequiredResource() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(missingRevocationsManifestJson()));
    }

    @Test
    void rejectsUnsafePath() {
        String json = validManifestJson().replace("artifacts/catalog/index-test.json.zst", "../index.json.zst");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(json));
    }

    private static String missingRevocationsManifestJson() {
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
                "integrity": {"algorithm":"sha256","digest":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","bytes":128}
              }
            }
          }
        }
        """;
    }

    private static String validManifestJson() {
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
                    "integrity": {"algorithm":"sha256","digest":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","bytes":128}
                  }
                },
                "revocations": {
                  "index": {
                    "path": "artifacts/revocations/index-test.json.zst",
                    "contentType": "application/vnd.heads.revocations-index+json",
                    "compression": "zstd",
                    "integrity": {"algorithm":"sha256","digest":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb","bytes":64}
                  }
                }
              }
            }
            """;
    }
}
