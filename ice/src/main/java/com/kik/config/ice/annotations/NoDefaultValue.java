package com.kik.config.ice.annotations;

import com.kik.config.ice.internal.annotations.None;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a config method or interface of config methods are intentionally not specifying a default value.
 * If the return type of that method is an Optional, the default will be Optional.empty(). Otherwise, the default
 * will be null.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
@Documented
public @interface NoDefaultValue
{
    /**
     * Define this when the return type has an inner type, such as when the return type is an Optional or List
     */
    Class<?> innerType() default None.class;
}
