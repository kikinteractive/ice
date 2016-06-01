/*
 * Copyright 2016 Kik Interactive, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
