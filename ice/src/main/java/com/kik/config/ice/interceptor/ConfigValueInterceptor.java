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
