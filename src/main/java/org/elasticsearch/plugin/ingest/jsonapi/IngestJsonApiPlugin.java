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

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.cache.Cache;
import org.elasticsearch.common.cache.CacheBuilder;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.ingest.Processor;
import org.elasticsearch.plugins.IngestPlugin;
import org.elasticsearch.plugins.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IngestJsonApiPlugin extends Plugin implements IngestPlugin {
    private final Setting<Long> CACHE_SIZE_SETTING = Setting.longSetting("ingest.json-api.cache_size", 1000, 0,
            Setting.Property.NodeScope);

    @Override
    public List<Setting<?>> getSettings() {
        return Collections.singletonList(CACHE_SIZE_SETTING);
    }

    @Override
    public Map<String, Processor.Factory> getProcessors(Processor.Parameters parameters) {
        long cacheSize = CACHE_SIZE_SETTING.get(parameters.env.settings());
        Cache<String, String> cache = CacheBuilder.<String, String>builder().setMaximumWeight(cacheSize).build();
        Logger logger = Loggers.getLogger(IngestJsonApiPlugin.class, "IngestJsonApi");
        logger.info("Created cache with size " + cacheSize);
        return MapBuilder.<String, Processor.Factory>newMapBuilder()
                .put(JsonApiProcessor.TYPE, new JsonApiProcessor.Factory(cache))
                .immutableMap();
    }

}
