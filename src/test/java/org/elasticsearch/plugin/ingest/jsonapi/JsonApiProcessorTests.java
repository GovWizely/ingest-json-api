/*
 * Copyright [2017] [Loren Siebert]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.elasticsearch.plugin.ingest.jsonapi;

import org.apache.http.client.ClientProtocolException;
import org.elasticsearch.common.cache.Cache;
import org.elasticsearch.common.cache.CacheBuilder;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESTestCase;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class JsonApiProcessorTests extends ESTestCase {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final long TEST_CACHE_SIZE = 1;

    public void testThatProcessorWorks() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("ip", "216.102.95.101");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        Cache<String, String> cache = CacheBuilder.<String, String>builder().setMaximumWeight(TEST_CACHE_SIZE).build();

        JsonApiProcessor processor = new JsonApiProcessor(randomAlphaOfLength(10), "ip", "country",
                "http://ip-api.com/json/{}", null, true, "country", false, cache);
        Map<String, Object> data = processor.execute(ingestDocument).getSourceAndMetadata();

        assertThat(data, hasKey("country"));
        assertThat(data.get("country"), is("United States"));
    }

    public void testOverwritingSourceField() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("foo", "216.102.95.101");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        Cache<String, String> cache = CacheBuilder.<String, String>builder().setMaximumWeight(TEST_CACHE_SIZE).build();

        JsonApiProcessor processor = new JsonApiProcessor(randomAlphaOfLength(10), "foo", "foo",
                "http://ip-api.com/json/{}", null, true, "country", false, cache);
        Map<String, Object> data = processor.execute(ingestDocument).getSourceAndMetadata();

        assertThat(data, hasKey("foo"));
        assertThat(data.get("foo"), is("United States"));
    }

    public void testExtraHeader() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("ip", "216.102.95.101");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        Cache<String, String> cache = CacheBuilder.<String, String>builder().setMaximumWeight(TEST_CACHE_SIZE).build();

        JsonApiProcessor processor = new JsonApiProcessor(randomAlphaOfLength(10), "ip", "country",
                "http://ip-api.com/json/{}", "Authorization: Basic ABC123==",
                true, "country", false, cache);
        Map<String, Object> data = processor.execute(ingestDocument).getSourceAndMetadata();

        assertThat(data, hasKey("country"));
        assertThat(data.get("country"), is("United States"));
    }

    public void testMultiValue() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("ip", "216.102.95.101");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        Cache<String, String> cache = CacheBuilder.<String, String>builder().setMaximumWeight(TEST_CACHE_SIZE).build();

        JsonApiProcessor processor = new JsonApiProcessor(randomAlphaOfLength(10), "ip", "country",
                "http://ip-api.com/json/{}", null, true, "country", true, cache);
        Map<String, Object> data = processor.execute(ingestDocument).getSourceAndMetadata();

        assertThat(data, hasKey("country"));
        assertThat(data.get("country"), is(Collections.singletonList("United States")));
    }

    public void testIgnoreMissingWithNull() throws Exception {
        Map<String, Object> document = new HashMap<>();
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        Cache<String, String> cache = CacheBuilder.<String, String>builder().setMaximumWeight(TEST_CACHE_SIZE).build();

        JsonApiProcessor processor = new JsonApiProcessor(randomAlphaOfLength(10), "ip", "country",
                "http://ip-api.com/json/{}", null, true, "country", false, cache);
        Map<String, Object> data = processor.execute(ingestDocument).getSourceAndMetadata();

        assertThat(data, not(hasKey("country")));
    }

    public void testDoNotIgnoreMissing() throws Exception {
        Map<String, Object> document = new HashMap<>();
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        Cache<String, String> cache = CacheBuilder.<String, String>builder().setMaximumWeight(TEST_CACHE_SIZE).build();

        JsonApiProcessor processor = new JsonApiProcessor(randomAlphaOfLength(10), "ip", "country",
                "http://ip-api.com/json/{}", null, false, "country", false, cache);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("field [ip] not present as part of path [ip]");
        processor.execute(ingestDocument);
    }

    public void testResponseHandler() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("country", "Elbonia");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        Cache<String, String> cache = CacheBuilder.<String, String>builder().setMaximumWeight(TEST_CACHE_SIZE).build();

        JsonApiProcessor processor = new JsonApiProcessor(randomAlphaOfLength(10), "country", "country",
                "http://restcountries.eu/rest/v1/name/{}", null, true, "$..alpha2Code", false, cache);
        thrown.expect(ClientProtocolException.class);
        thrown.expectMessage("Unexpected response status: 404");
        processor.execute(ingestDocument);
    }

    public void testUrlEncodingOfSpaces() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("country", "United States");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        Cache<String, String> cache = CacheBuilder.<String, String>builder().setMaximumWeight(TEST_CACHE_SIZE).build();

        JsonApiProcessor processor = new JsonApiProcessor(randomAlphaOfLength(10), "country", "country",
                "https://restcountries.eu/rest/v1/name/{}?fullText=true", null, true, "$..alpha2Code", false, cache);
        Map<String, Object> data = processor.execute(ingestDocument).getSourceAndMetadata();

        assertThat(data, hasKey("country"));
        assertThat(data.get("country"), is("US"));
    }
}

