package com.kik.config.ice.naming;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Singleton;
import java.lang.reflect.Method;
import java.util.Optional;

@Singleton
public class SimpleConfigNamingStrategy implements ConfigNamingStrategy
{
    @Override
    public String methodToFlatName(Method method, Optional<String> scopeOpt)
    {
        final String className = method.getDeclaringClass().getName();
        final String methodName = method.getName();
        return scopeOpt.isPresent()
            ? String.format("%s.%s:%s", className, methodName, scopeOpt.get())
            : String.format("%s.%s", className, methodName);
    }

    public static Module module()
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind(ConfigNamingStrategy.class).to(SimpleConfigNamingStrategy.class);
            }
        };
    }
}
