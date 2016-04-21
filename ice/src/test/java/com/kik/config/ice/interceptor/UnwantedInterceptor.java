package com.kik.config.ice.interceptor;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.multibindings.MapBinder;
import java.lang.reflect.Method;
import java.util.Optional;

public class UnwantedInterceptor implements ConfigValueInterceptor
{
    @Override
    public boolean shouldApply(Method configMethod, Optional<String> inputOpt)
    {
        return inputOpt.isPresent() && inputOpt.get().startsWith("abcd");
    }

    @Override
    public Optional<String> apply(Optional<String> inputOpt)
    {
        return Optional.of("BAD OUTPUT");
    }

    @Override
    public boolean stopChainOnApply()
    {
        return false;
    }

    public static Module module(final int priority)
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                MapBinder<Integer, ConfigValueInterceptor> mapBinder = MapBinder.newMapBinder(binder(), Integer.class, ConfigValueInterceptor.class);
                mapBinder.addBinding(priority).to(UnwantedInterceptor.class);
            }
        };
    }
}
