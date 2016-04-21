package com.kik.config.ice.internal.annotations;

import static com.google.common.base.Preconditions.checkNotNull;
import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * Implementation of annotation used to define the annotation at runtime.
 */
@SuppressWarnings("AnnotationAsSuperInterface")
public class PropertyIdentifierImpl implements PropertyIdentifier
{
    private final String propertyName;
    private final Class<?> configClass;

    public PropertyIdentifierImpl(String propertyName, Class<?> configClass)
    {
        this.propertyName = checkNotNull(propertyName);
        this.configClass = checkNotNull(configClass);
    }

    @Override
    public Class<?> configClass()
    {
        return configClass;
    }

    @Override
    public String propertyName()
    {
        return propertyName;
    }

    @Override
    public Class<? extends Annotation> annotationType()
    {
        return PropertyIdentifier.class;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(propertyName, configClass);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PropertyIdentifierImpl that = (PropertyIdentifierImpl) obj;
        return Objects.equals(this.propertyName, that.propertyName)
            && Objects.equals(this.configClass, that.configClass);
    }

    @Override
    public String toString()
    {
        return String.format("PropertyIdentifierImpl{name=%s, class=%s}", this.propertyName, this.configClass.getName());
    }
}
