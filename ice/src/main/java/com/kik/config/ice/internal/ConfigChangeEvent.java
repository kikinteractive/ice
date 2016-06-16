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
