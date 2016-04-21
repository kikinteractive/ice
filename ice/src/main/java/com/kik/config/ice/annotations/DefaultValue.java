package com.kik.config.ice.annotations;

import com.kik.config.ice.internal.annotations.None;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the default value for the annotated config method in string form.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface DefaultValue
{
    /**
     * String version of the default value
     */
    String value();

    /**
     * Define this when the return type has an inner type, such as when the return type is an Optional or List
     */
    Class<?> innerType() default None.class;
}
