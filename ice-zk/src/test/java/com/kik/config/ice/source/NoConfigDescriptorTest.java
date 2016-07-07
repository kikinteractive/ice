package com.kik.config.ice.source;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.zookeeper.ZooKeeperServerRule;
import org.junit.ClassRule;
import org.junit.Test;

public class NoConfigDescriptorTest
{
    @ClassRule
    public static final ZooKeeperServerRule serverRule = new ZooKeeperServerRule();

    @Test(timeout = 5000)
    public void testNoConfigDescriptors()
    {
        Injector injector = Guice.createInjector(ConfigConfigurator.testModules(), ZooKeeperDynamicConfigSource.module(), new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind(String.class).annotatedWith(Names.named(ZooKeeperDynamicConfigSource.CONFIG_CONNECTION_STRING)).toInstance(serverRule.getConnectionString());
            }
        });

        injector.injectMembers(this);
    }
}
