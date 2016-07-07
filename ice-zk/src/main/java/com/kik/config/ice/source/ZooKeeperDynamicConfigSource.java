/*
 * Copyright 2016 Kik Interactive, Inc.
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
package com.kik.config.ice.source;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.kik.config.ice.internal.ConfigDescriptor;
import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.ensemble.EnsembleProvider;
import org.apache.curator.ensemble.fixed.FixedEnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.nodes.PersistentEphemeralNode;
import static org.apache.curator.framework.recipes.nodes.PersistentEphemeralNode.Mode.EPHEMERAL;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import static org.apache.curator.utils.ZKPaths.makePath;

@Slf4j
@Singleton
public class ZooKeeperDynamicConfigSource extends AbstractDynamicConfigSource implements Closeable
{
    private static final String CONFIG_PREFIX = "dynamic_config_source_zookeeper_";

    public static final String CONFIG_CONNECTION_STRING = CONFIG_PREFIX + "connection_string";
    public static final String CONFIG_CURATOR_NAMESPACE = CONFIG_PREFIX + "curator_namespace";
    /**
     * Note that the zookeeper server will impose upper and lower bounds on this based on the tickTime and
     * {min/max}SessionTimeout settings.
     */
    public static final String CONFIG_CURATOR_SESSION_TIMEOUT = CONFIG_PREFIX + "curator_session_timeout";
    public static final String CONFIG_CURATOR_CONNECTION_TIMEOUT = CONFIG_PREFIX + "curator_connection_timeout";
    public static final String CONFIG_CURATOR_RETRY_BASE_TIME = CONFIG_PREFIX + "curator_retry_base_time";
    public static final String CONFIG_CURATOR_RETRY_MAX_TIME = CONFIG_PREFIX + "curator_retry_max_time";
    public static final String CONFIG_CURATOR_RETRY_LIMIT = CONFIG_PREFIX + "curator_retry_limit";

    // Defaults
    // NOTE: No default for connection string.
    @VisibleForTesting
    static final String DEFAULT_CURATOR_NAMESPACE = "app";
    private static final int DEFAULT_CURATOR_SESSION_TIMEOUT = 30_000;
    private static final int DEFAULT_CURATOR_CONNECTION_TIMEOUT = 5_000;
    private static final int DEFAULT_CURATOR_RETRY_BASE_TIME = 200;
    private static final int DEFAULT_CURATOR_RETRY_MAX_TIME = 1_000;
    private static final int DEFAULT_CURATOR_RETRY_LIMIT = 3;

    private static final int CONFIG_SOURCE_PRIORITY_DEFAULT = 50;
    @VisibleForTesting
    static final String ROOT_ZK_PATH = "/config/overrides";

    private final String connectionString;
    private final String namespace;
    private final int sessionTimeout;
    private final int connectionTimeout;
    private final int retryBaseTime;
    private final int retryMaxTime;
    private final int retryLimit;
    final String localNodeName;
    private CuratorFramework curator;
    private final Map<ConfigDescriptor, NodeCache> configNodeCaches = Maps.newConcurrentMap();
    private final Map<ConfigDescriptor, PersistentEphemeralNode> ephemeralNodes = Maps.newConcurrentMap();

    /**
     * Note: Constructor used by {@link ZooKeeperDynamicConfigSourceProvider} only
     *
     * @param configDescriptors
     * @param connectionString
     */
    private ZooKeeperDynamicConfigSource(
        Set<ConfigDescriptor> configDescriptors,
        String connectionString, String namespace,
        int sessionTimeout, int connectionTimeout,
        int retryBaseTime, int retryMaxTime, int retryLimit)
    {
        super(configDescriptors);

        this.connectionString = connectionString;
        this.namespace = namespace;
        this.sessionTimeout = sessionTimeout;
        this.connectionTimeout = connectionTimeout;
        this.retryBaseTime = retryBaseTime;
        this.retryMaxTime = retryMaxTime;
        this.retryLimit = retryLimit;

        this.localNodeName = getLocalNodeName();
        initializeCurator();
    }

    private void initializeCurator()
    {
        // Create/start CuratorFramework client
        RetryPolicy retryPolicy = new BoundedExponentialBackoffRetry(retryBaseTime, retryMaxTime, retryLimit);
        EnsembleProvider ensembleProvider = new FixedEnsembleProvider(connectionString);
        curator = CuratorFrameworkFactory.builder()
            .ensembleProvider(ensembleProvider)
            .retryPolicy(retryPolicy)
            .namespace(namespace)
            .sessionTimeoutMs(sessionTimeout)
            .connectionTimeoutMs(connectionTimeout)
            .build();
        curator.start();

        // Create a NodeCache for each config descriptor
        for (ConfigDescriptor desc : configDescriptors) {
            final String configPath = makePath(ROOT_ZK_PATH, desc.getConfigName());
            final NodeCache nc = new NodeCache(curator, configPath);
            nc.getListenable().addListener(() -> onNodeChanged(nc, desc));
            try {
                nc.start(true);

                // Note that we have to force calling onNodeChanged() here since `nc.start(true)` will not emit an initial event.
                onNodeChanged(nc, desc);

                // Create the ephemeral node last, just in case something goes wrong with seting up the node cache
                PersistentEphemeralNode en = new PersistentEphemeralNode(curator, EPHEMERAL, makePath(configPath, localNodeName), new byte[0]);
                en.start();
                ephemeralNodes.put(desc, en);
            }
            catch (Exception ex) {
                log.warn("Failed to initialize for configPath {}", configPath, ex);
            }
            this.configNodeCaches.put(desc, nc);
        }
    }

    private static String getLocalNodeName()
    {
        try {
            // returns pid@hostname - we want to reverse this.
            String[] parts = ManagementFactory.getRuntimeMXBean().getName().split("@");
            return String.format("%s@%s", parts[1], parts[0]);
        }
        catch (Exception ex) {
            log.warn("Unable to get host/pid via RuntimeMXBean", ex);
        }

        try {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (Exception ex) {
            log.warn("Unable to get hostname via InetAddress", ex);
        }

        return "Unknown";
    }

    @Override
    public void close() throws IOException
    {
        for (Map.Entry<ConfigDescriptor, PersistentEphemeralNode> entry : ephemeralNodes.entrySet()) {
            try {
                entry.getValue().close();
            }
            catch (Exception ex) {
                log.info("Failed to close PersistentEphemeralNode for configName {}", entry.getKey().getConfigName(), ex);
            }
        }
        ephemeralNodes.clear();

        for (Map.Entry<ConfigDescriptor, NodeCache> entry : configNodeCaches.entrySet()) {
            try {
                entry.getValue().close();
            }
            catch (Exception ex) {
                log.info("Failed to close NodeCache for configName {}", entry.getKey().getConfigName(), ex);
            }
        }
        configNodeCaches.clear();

        curator.close();
        curator = null;
    }

    public void onNodeChanged(final NodeCache cache, final ConfigDescriptor desc)
    {
        ChildData childData = cache.getCurrentData();
        try {
            Optional<String> valueOpt = Optional.empty();
            if (childData != null && childData.getData() != null && childData.getData().length > 0) {
                valueOpt = Optional.of(new String(childData.getData(), Charsets.UTF_8));
            }
            emitEvent(desc.getConfigName(), valueOpt);
        }
        catch (Exception ex) {
            log.warn("Failed to handle onNodeChanged w/ new data for config key {}, data {}", desc.getConfigName(), childData, ex);
        }
    }

    public static class ZooKeeperDynamicConfigSourceProvider implements Provider<ZooKeeperDynamicConfigSource>
    {
        @Inject(optional = true)
        private Set<ConfigDescriptor> configDescriptors;

        @Inject
        @Named(CONFIG_CONNECTION_STRING)
        private String connectionString;

        @Inject(optional = true)
        @Named(CONFIG_CURATOR_NAMESPACE)
        private String namespace;

        @Inject(optional = true)
        @Named(CONFIG_CURATOR_SESSION_TIMEOUT)
        private Integer sessionTimeout;

        @Inject(optional = true)
        @Named(CONFIG_CURATOR_CONNECTION_TIMEOUT)
        private Integer connectionTimeout;

        @Inject(optional = true)
        @Named(CONFIG_CURATOR_RETRY_BASE_TIME)
        private Integer retryBaseTime;

        @Inject(optional = true)
        @Named(CONFIG_CURATOR_RETRY_MAX_TIME)
        private Integer retryMaxTime;

        @Inject(optional = true)
        @Named(CONFIG_CURATOR_RETRY_LIMIT)
        private Integer retryLimit;

        private void fillInDefaults()
        {
            if (namespace == null) {
                namespace = DEFAULT_CURATOR_NAMESPACE;
            }
            if (sessionTimeout == null) {
                sessionTimeout = DEFAULT_CURATOR_SESSION_TIMEOUT;
            }
            if (connectionTimeout == null) {
                connectionTimeout = DEFAULT_CURATOR_CONNECTION_TIMEOUT;
            }
            if (retryBaseTime == null) {
                retryBaseTime = DEFAULT_CURATOR_RETRY_BASE_TIME;
            }
            if (retryMaxTime == null) {
                retryMaxTime = DEFAULT_CURATOR_RETRY_MAX_TIME;
            }
            if (retryLimit == null) {
                retryLimit = DEFAULT_CURATOR_RETRY_LIMIT;
            }
        }

        @Override
        public ZooKeeperDynamicConfigSource get()
        {
            fillInDefaults();
            return new ZooKeeperDynamicConfigSource(
                configDescriptors,
                connectionString, namespace,
                sessionTimeout, connectionTimeout,
                retryBaseTime, retryMaxTime, retryLimit);
        }
    }

    public static Module module()
    {
        return module(CONFIG_SOURCE_PRIORITY_DEFAULT);
    }

    public static Module module(final int configSourcePriority)
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                MapBinder<Integer, DynamicConfigSource> mapBinder = MapBinder.newMapBinder(binder(), Integer.class, DynamicConfigSource.class);
                mapBinder.addBinding(configSourcePriority).toProvider(ZooKeeperDynamicConfigSourceProvider.class).in(Scopes.SINGLETON);
            }
        };
    }
}
