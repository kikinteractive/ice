package com.kik.config.ice.internal.annotations;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

@Retention(RUNTIME)
@Target({})
@Qualifier
public @interface PropertyIdentifier
{
    Class<?> configClass();

    String propertyName();
}
