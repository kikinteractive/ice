package com.kik.config.ice.internal;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Optional;
import lombok.Data;
import lombok.NonNull;

@Data
public class ConfigChangeEvent<T>
{
    @NonNull
    private final String name;
    @NonNull
    private final Optional<T> valueOpt;
    private final long timestamp;

    public ConfigChangeEvent(String name, Optional<T> valueOpt)
    {
        this.name = checkNotNull(name);
        this.valueOpt = checkNotNull(valueOpt);
        this.timestamp = System.currentTimeMillis();
    }
}
