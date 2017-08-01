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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.ingest.ConfigurationUtils.readBooleanProperty;
import static org.elasticsearch.ingest.ConfigurationUtils.readStringProperty;

public final class JsonApiProcessor extends AbstractProcessor {

    static final String TYPE = "json_api";

    private final String field;
    private final String urlPrefix;
    private final String targetField;
    private final String extraHeader;
    private final String jsonPath;
    private final boolean multiValue;

    private final Logger logger;

    private final boolean ignoreMissing;

    JsonApiProcessor(String tag, String field, String targetField, String urlPrefix, String extraHeader,
                     boolean ignoreMissing, String jsonPath, boolean multiValue) throws IOException {
        super(tag);
        this.field = field;
        this.urlPrefix = urlPrefix;
        this.targetField = targetField;
        this.ignoreMissing = ignoreMissing;
        this.extraHeader = extraHeader;
        this.jsonPath = jsonPath;
        this.multiValue = multiValue;
        this.logger = Loggers.getLogger(IngestJsonApiPlugin.class);
    }


    @Override
    public void execute(IngestDocument ingestDocument) throws java.io.IOException {

        String fieldValue = ingestDocument.getFieldValue(field, String.class, ignoreMissing);
        if (fieldValue == null) {
            if (ignoreMissing) {
                return;
            } else {
                throw new IllegalArgumentException("field [" + field + "] is null, cannot extract URL.");
            }
        }

        String url = urlPrefix.replace("{}", URLEncoder.encode(fieldValue, "UTF-8"));
        logger.debug("url: " + url);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);

            // TODO: remove or simplify
            if (extraHeader != null) {
                if (extraHeader.indexOf(":") > 0) {
                    httpGet.addHeader(extraHeader.substring(0, extraHeader.indexOf(":")).trim(),
                            extraHeader.substring(extraHeader.indexOf(":") + 1).trim());
                }
            }

            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };

            String responseBody = httpClient.execute(httpGet, responseHandler);
            logger.debug("responseBody: " + responseBody);
            Configuration conf = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);
            List<Object> valueAtPath = JsonPath.using(conf).parse(responseBody).read(jsonPath);

            ingestDocument.setFieldValue(targetField, multiValue ? valueAtPath : valueAtPath.get(0));
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public static final class Factory implements Processor.Factory {

        @Override
        public JsonApiProcessor create(Map<String, Processor.Factory> registry, String processorTag,
                                       Map<String, Object> config) throws Exception {
            String field = readStringProperty(TYPE, processorTag, config, "field");
            String urlPrefix = readStringProperty(TYPE, processorTag, config, "url_prefix");
            String targetField = readStringProperty(TYPE, processorTag, config, "target_field", "out");
            String extraHeader = readStringProperty(TYPE, processorTag, config, "extra_header", "");
            boolean ignoreMissing = readBooleanProperty(TYPE, processorTag, config, "ignore_missing", false);
            String jsonPath = readStringProperty(TYPE, processorTag, config, "json_path", "$..*");
            boolean multiValue = readBooleanProperty(TYPE, processorTag, config, "multi_value", false);

            return new JsonApiProcessor(processorTag, field, targetField, urlPrefix, extraHeader, ignoreMissing,
                    jsonPath, multiValue);
        }
    }

}
