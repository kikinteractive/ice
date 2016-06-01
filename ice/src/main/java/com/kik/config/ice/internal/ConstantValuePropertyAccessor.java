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

public class ConstantValuePropertyAccessor
{
    private final Optional<String> valueOpt;
    private final Optional<Object> rawValueOpt;

    /**
     * Build a {@link ConstantValuePropertyAccessor}, given an optional String value
     *
     * @param valueOpt string value to be converted to use as the constant/default value
     * @return a new instance of {@link ConstantValuePropertyAccessor}
     */
    public static ConstantValuePropertyAccessor fromStringOpt(Optional<String> valueOpt)
    {
        return new ConstantValuePropertyAccessor(valueOpt, Optional.empty());
    }

    /**
     * Build a {@link ConstantValuePropertyAccessor}, given a raw Object to cast and return.
     *
     * @param rawValue object value to use directly as the constant/default value
     * @return a new instance of {@link ConstantValuePropertyAccessor}
     */
    public static ConstantValuePropertyAccessor fromRawValue(Object rawValue)
    {
        checkNotNull(rawValue);
        return new ConstantValuePropertyAccessor(Optional.of(rawValue.toString()), Optional.of(rawValue));
    }

    /**
     * Constructor that is given a string value (from annotation or configuration source). If the raw value
     * is present, it is preferred over the string value. This is done to enable type-safe overrides without
     * having to convert the value to a string and back again.
     *
     * @param valueOpt    string value to be converted to use as the constant/default value
     * @param rawValueOpt object value to use directly as the constant/default value
     */
    protected ConstantValuePropertyAccessor(Optional<String> valueOpt, Optional<Object> rawValueOpt)
    {
        this.valueOpt = checkNotNull(valueOpt);
        this.rawValueOpt = checkNotNull(rawValueOpt);
    }

    public Optional<String> getValue()
    {
        return this.valueOpt;
    }

    public Optional<Object> getRawValue()
    {
        return this.rawValueOpt;
    }
}
