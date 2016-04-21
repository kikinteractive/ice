package com.kik.config.ice.example;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;

@Singleton
public class Example3A
{
    public static final String OTHER_NAME = "ex3a_specific";

    public interface Config
    {
        @DefaultValue("5000")
        Integer cacheMaxSize();
    }

    @Inject
    @VisibleForTesting
    Config config;

    @Inject
    @VisibleForTesting
    @Named(OTHER_NAME)
    Example3B otherComponent;

    public static Module module()
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                // setup config impl for this class
                install(ConfigSystem.configModule(Example3A.Config.class));

                install(Example3B.module(Names.named(OTHER_NAME)));

                // setup named config impl for other component
                install(ConfigSystem.configModuleWithOverrides(Example3B.Config.class, Names.named(OTHER_NAME), om -> {
                    om.override(om.id().enabled()).withValue(false);
                    om.override(om.id().maxQueueSize()).withValue(100);
                    om.override(om.id().threadCount()).withValue(12);
                }));
            }
        };
    }
}
