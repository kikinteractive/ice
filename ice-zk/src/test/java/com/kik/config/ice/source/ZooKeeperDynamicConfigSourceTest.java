package com.kik.config.ice.source;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.base.Charsets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.internal.ConfigBuilder;
import com.kik.config.ice.internal.ConfigDescriptor;
import com.kik.config.ice.internal.ConstantValuePropertyAccessor;
import com.kik.config.ice.internal.OverrideModule;
import com.kik.config.ice.internal.PropertyAccessor;
import com.kik.zookeeper.ZooKeeperServerRule;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.ensemble.EnsembleProvider;
import org.apache.curator.ensemble.fixed.FixedEnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.data.Stat;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

@Slf4j
public class ZooKeeperDynamicConfigSourceTest
{
    public static void setLogLevel(Class<?> loggingClass, Level logLevel)
    {
        Logger logger = (Logger) LoggerFactory.getLogger(loggingClass);
        logger.setLevel(logLevel);
    }

    public static void setLogLevel(String loggerName, Level logLevel)
    {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
        logger.setLevel(logLevel);
    }

    static {
        // Logging setup
        setLogLevel("org.apache.zookeeper", Level.WARN);
        setLogLevel("com.kik.config2.source", Level.TRACE);

        setLogLevel(ConfigBuilder.class, Level.TRACE);
        setLogLevel(OverrideModule.class, Level.TRACE);
        setLogLevel(PropertyAccessor.class, Level.TRACE);
        setLogLevel(ConstantValuePropertyAccessor.class, Level.TRACE);
    }

    @ClassRule
    public static final ZooKeeperServerRule serverRule = new ZooKeeperServerRule();

    private static CuratorFramework curator;

    @Inject
    ExampleComponent example;

    @Inject
    Set<ConfigDescriptor> configDescriptors;

    @BeforeClass
    public static void setupTestCurator()
    {
        RetryPolicy retryPolicy = new BoundedExponentialBackoffRetry(200, 1000, 3);
        EnsembleProvider ensembleProvider = new FixedEnsembleProvider(serverRule.getConnectionString());
        curator = CuratorFrameworkFactory.builder()
            .ensembleProvider(ensembleProvider)
            .retryPolicy(retryPolicy)
            .namespace(ZooKeeperDynamicConfigSource.DEFAULT_CURATOR_NAMESPACE)
            .sessionTimeoutMs(10_000)
            .connectionTimeoutMs(5_000)
            .build();
        curator.start();
    }

    @AfterClass
    public static void teardownCurator()
    {
        curator.close();
    }

    @Before
    public void setup()
    {
        getSetupInjector().injectMembers(this);
    }

    private Injector getSetupInjector()
    {
        return Guice.createInjector(
            ConfigConfigurator.testModules(),
            ZooKeeperDynamicConfigSource.module(),
            ExampleComponent.module(),
            new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    log.debug("ServerRule ConnectionString is: {}", serverRule.getConnectionString());
                    bind(String.class).annotatedWith(Names.named(ZooKeeperDynamicConfigSource.CONFIG_CONNECTION_STRING)).toInstance(serverRule.getConnectionString());
                }
            });
    }

    private void assertComponentDefaults()
    {
        assertNotNull(example);
        assertNotNull(example.config);
        assertEquals(true, example.config.enabled());
        assertNull(example.config.maxPageSize());
        assertEquals(Optional.empty(), example.config.connectionString());
        assertNotNull(example.config.hostnames());
        assertEquals("a", example.config.hostnames().get(0));
        assertEquals("b", example.config.hostnames().get(1));
        assertEquals("c", example.config.hostnames().get(2));

        assertNotNull(example.subComp);
        assertNotNull(example.subComp.config);
        assertEquals(5, example.subComp.config.maxRetries());
        assertEquals(10_000L, example.subComp.config.defaultTimeout());
        assertEquals(Duration.parse("PT5M30S"), example.subComp.config.expiry());

        assertNotNull(configDescriptors);
        assertEquals(7, configDescriptors.size());
    }

    private void assertAllNodesExistAndEmpty() throws Exception
    {
        for (ConfigDescriptor desc : configDescriptors) {
            final String configPath = ZKPaths.makePath(ZooKeeperDynamicConfigSource.ROOT_ZK_PATH, desc.getConfigName());
            Stat stat = curator.checkExists().forPath(configPath);
            assertNotNull(stat);

            byte[] data = curator.getData().forPath(configPath);
            String dataStr = null;
            if (data != null && data.length > 0) {
                dataStr = new String(data, Charsets.UTF_8);
            }
            assertTrue(String.format("Data exists where null expected. key: %s, data: %s", desc.getConfigName(), dataStr),
                data == null || data.length == 0);
        }
    }

    // Note: this only works because ExampleComponent and ExampleSubComponent each have distinct names for each config entry.
    private Optional<ConfigDescriptor> findByMethodName(String methodName)
    {
        return configDescriptors.stream()
            .filter(d -> d.getMethod().getName().equals(methodName))
            .findFirst();
    }

    private void setData(ConfigDescriptor desc, String value) throws Exception
    {
        final String configPath = ZKPaths.makePath(ZooKeeperDynamicConfigSource.ROOT_ZK_PATH, desc.getConfigName());
        byte[] data = value == null ? null : value.getBytes(Charsets.UTF_8);
        curator.setData().forPath(configPath, data);
    }

    /**
     * NOTE: Sleeps allow time for event propagation
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    @SuppressWarnings("UnnecessaryUnboxing")
    public void testBasicFunctionality() throws Exception
    {
        assertComponentDefaults();
        assertAllNodesExistAndEmpty();

        setData(findByMethodName("enabled").get(), "false");
        Thread.sleep(100);
        assertEquals(false, example.config.enabled());

        ConfigDescriptor maxPageSizeDesc = findByMethodName("maxPageSize").get();
        setData(maxPageSizeDesc, "1122");
        Thread.sleep(100);
        assertEquals(1122, example.config.maxPageSize().intValue());

        setData(maxPageSizeDesc, "");
        Thread.sleep(100);
        assertNull(example.config.maxPageSize());

        setData(findByMethodName("expiry").get(), "PT1H15S");
        Thread.sleep(100);
        assertEquals(Duration.parse("PT1H15S"), example.subComp.config.expiry());

        setData(findByMethodName("expiry").get(), null);
        Thread.sleep(100);
        assertEquals(Duration.parse("PT5M30S"), example.subComp.config.expiry());

        // Now let's emulate a *new* node coming online. We want to ensure
        // that it will load the overrides right away.
        example = getSetupInjector().getInstance(ExampleComponent.class);
        assertEquals(false, example.config.enabled());
    }
}
