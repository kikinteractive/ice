package com.kik.config.ice.example;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

@Singleton
public class ObservableExample
{
    private static final Logger log = LoggerFactory.getLogger(ObservableExample.class);

    public interface Config
    {
        @DefaultValue("true")
        Boolean enabled();

        @DefaultValue("100")
        Integer maxSize();

        Observable<Boolean> enabledObservable();

        Observable<Integer> maxSizeObservable();
    }

    @VisibleForTesting
    @Inject
    Config config;

    public static Module module()
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind(ObservableExample.class);
                install(ConfigSystem.configModule(Config.class));
            }
        };
    }

    public void fooSetup()
    {
        rx.Subscription maxSizeSubscription = config.maxSizeObservable().subscribe(maxSize -> log.info("Max Size changed to: {}", maxSize));
    }
}
