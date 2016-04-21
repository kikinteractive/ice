package com.kik.config.ice.internal.annotations;

import com.google.inject.Singleton;
import java.lang.annotation.Annotation;

/**
 * Implementation of annotation used to define the annotation at runtime.
 */
@SuppressWarnings("AnnotationAsSuperInterface")
public class SingletonImpl implements Singleton
{
    @Override
    public Class<? extends Annotation> annotationType()
    {
        return Singleton.class;
    }

}
