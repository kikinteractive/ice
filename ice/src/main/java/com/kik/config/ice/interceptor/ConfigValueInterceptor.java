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
package com.kik.config.ice.interceptor;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Interface allowing values to be intercepted from config sources, and modified and/or filtered as needed.
 */
public interface ConfigValueInterceptor
{
    /**
     * Detects if this filter should be applied to the value provided
     *
     * @param configMethod the method of the config interface
     * @param inputOpt     the input value to potentially be filtered and/or updated
     * @return true if this interceptor should be applied to the given method reference
     */
    boolean shouldApply(Method configMethod, Optional<String> inputOpt);

    /**
     * Applies this filter to the given input, providing the actual value to use in configuration.
     *
     * @param inputOpt the input value to be filtered / updated
     * @return the string (not-yet-converted) version of the configuration value, after having had this interceptor
     *         applied to it.
     */
    Optional<String> apply(Optional<String> inputOpt);

    /**
     * Determine if interceptors should be no longer applied if this interceptor was applied.
     *
     * @return true if no further interceptors should be applied after this one.
     */
    boolean stopChainOnApply();
}
