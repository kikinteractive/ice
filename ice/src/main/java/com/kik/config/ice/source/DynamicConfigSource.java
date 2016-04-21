package com.kik.config.ice.source;

import com.kik.config.ice.internal.ConfigChangeEvent;
import rx.Observable;

public interface DynamicConfigSource
{
    default String getName()
    {
        return getClass().getSimpleName();
    }

    Observable<ConfigChangeEvent<String>> getObservable(String configName);
}
