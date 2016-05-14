/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.ivyservice.ivyresolve.memcache;

import com.google.common.collect.MapMaker;
import org.gradle.api.internal.artifacts.ComponentMetadataProcessor;
import org.gradle.api.internal.artifacts.configurations.ResolutionStrategyInternal;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ModuleComponentRepository;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.internal.concurrent.Stoppable;

import java.util.Map;

/**
 * Caches the dependency metadata (descriptors, artifact files) in memory.
 */
public class InMemoryCachedRepositoryFactory implements Stoppable {

    public final static String TOGGLE_PROPERTY = "org.gradle.resolution.memorycache";

    private final static Logger LOG = Logging.getLogger(InMemoryCachedRepositoryFactory.class);
    private final CrossBuildModuleComponentCache crossBuildCache;

    Map<String, InMemoryModuleComponentRepositoryCaches> cachePerRepo = new MapMaker().makeMap();

    final InMemoryCacheStats stats = new InMemoryCacheStats();

    public InMemoryCachedRepositoryFactory(CrossBuildModuleComponentCache cache) {
        this.crossBuildCache = cache;
    }

    public ModuleComponentRepository cached(ResolutionStrategyInternal strategy, ComponentMetadataProcessor metadataProcessor, ModuleComponentRepository input) {
        if ("false".equalsIgnoreCase(System.getProperty(TOGGLE_PROPERTY))) {
            return input;
        }

        String cacheId = input.getId();
        InMemoryModuleComponentRepositoryCaches caches = cachePerRepo.get(cacheId);
        stats.reposWrapped++;
        if (caches == null) {
            LOG.debug("Creating new in-memory cache for repo '{}' [{}].", input.getName(), cacheId);
            caches = new InMemoryModuleComponentRepositoryCaches(stats);
            stats.cacheInstances++;
            cachePerRepo.put(cacheId, caches);
        } else {
            LOG.debug("Reusing in-memory cache for repo '{}' [{}].", input.getName(), cacheId);
        }
        return new InMemoryCachedModuleComponentRepository(strategy, caches, input, crossBuildCache, metadataProcessor);
    }

    public void stop() {
        cachePerRepo.clear();
        LOG.debug("In-memory dependency metadata cache closed. {}", stats);
    }
}
