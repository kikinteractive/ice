package com.kik.config.ice.internal;

import static com.google.common.base.Preconditions.checkNotNull;
import com.kik.config.ice.annotations.DefaultValue;
import com.kik.config.ice.annotations.NoDefaultValue;
import java.lang.reflect.Method;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;

@Slf4j
public class StaticConfigHelper
{
    public static final String OBSERVABLE_METHOD_SUFFIX = "Observable";

    /**
     * Return value type for method validation.
     */
    public enum MethodValidationState
    {
        /**
         * Method is OK for config interface
         */
        OK,
        /**
         * Method is a default method; Allowed, but must be handled differently.
         */
        IS_DEFAULT,
        /**
         * Method has a return type that is not handled by the injected config type converters.
         */
        UNKNOWN_RETURN_TYPE,
        /**
         * Method has arguments, or is otherwise not correct for a config interface.
         */
        BAD_SIGNATURE,
        /**
         * Default value annotation is missing
         */
        MISSING_DEFAULT_VALUE,
        /**
         * Both @DefaultValue and @NoDefaultValue were defined
         */
        AMBIGUOUS_DEFAULT_VALUE,
        /**
         * Method returning Observable found without the correct naming suffix expected.
         */
        OBSERVABLE_WITH_INCORRECT_SUFFIX,
        /**
         * Method returning Observable found with no matching configuration method.
         */
        OBSERVABLE_WITH_NO_MATCHING_CONFIG_METHOD,
        /**
         * Error occurred while validating the method.
         */
        ERROR;
    }

    /**
     * Validates that a given method from a config interface has a valid signature.
     * NOTE: Code that uses this method may also need to validate that the Guice Injector has a registered type
     * converter for the return type.
     *
     * @param method a method reference to validate
     * @return a {@link MethodValidationState} based on the method provided.
     */
    public static MethodValidationState isValidConfigInterfaceMethod(Method method)
    {
        if (method.isDefault()) {
            log.debug("Method {} found to be a default method.", method.getName());
            return MethodValidationState.IS_DEFAULT;
        }
        if (method.getParameterCount() > 0) {
            log.warn("Method {} may not have parameters, but found to have {}", method.getName(), method.getParameterCount());
            return MethodValidationState.BAD_SIGNATURE;
        }

        // If method is an Observable
        if (Observable.class.isAssignableFrom(method.getReturnType())) {
            return isValidObservableReturningMethod(method);
        }
        return isValidNonObservableMethod(method);
    }

    private static MethodValidationState isValidNonObservableMethod(Method method)
    {
        // Get method annotations
        DefaultValue defaultValue = method.getAnnotation(DefaultValue.class);
        NoDefaultValue noDefaultValue = method.getAnnotation(NoDefaultValue.class);

        // Method cannot be annotated with both @DefaultValue and @NoDefaultValue
        if (defaultValue != null && noDefaultValue != null) {
            log.warn("Config method {}.{} is ambiguous - annotated with both @DefaultValue and @NoDefaultValue",
                method.getDeclaringClass().getName(), method.getName());
            return MethodValidationState.AMBIGUOUS_DEFAULT_VALUE;
        }

        // If method is not annotated with @NoDefaultValue, check if the class is.
        if (noDefaultValue == null) {
            noDefaultValue = method.getDeclaringClass().getAnnotation(NoDefaultValue.class);
        }

        if (defaultValue == null && method.getReturnType().isPrimitive()) {
            log.warn("Config method {}.{} must have a default value, since it returns a primitive value",
                method.getDeclaringClass().getName(), method.getName());
            return MethodValidationState.MISSING_DEFAULT_VALUE;
        }

        if ((defaultValue != null && defaultValue.value() != null) || noDefaultValue != null) {
            log.trace("Method {} passed validation.", method.getName());
            return MethodValidationState.OK;
        }
        log.warn("Config method {}.{} is missing a default value.",
            method.getDeclaringClass().getName(), method.getName());
        return MethodValidationState.MISSING_DEFAULT_VALUE;
    }

    private static MethodValidationState isValidObservableReturningMethod(Method method)
    {
        if (!method.getName().endsWith(OBSERVABLE_METHOD_SUFFIX)) {
            log.warn("Method {} returns an observable, but does not have the correct naming suffix of '{}'",
                method.getName(), OBSERVABLE_METHOD_SUFFIX);
            return MethodValidationState.OBSERVABLE_WITH_INCORRECT_SUFFIX;
        }

        // ensure there is another non-observable config method
        String otherMethodName = method.getName().substring(0, method.getName().length() - OBSERVABLE_METHOD_SUFFIX.length());
        Method otherMethod;
        try {
            otherMethod = method.getDeclaringClass().getMethod(otherMethodName);
        }
        catch (NoSuchMethodException ex) {
            log.warn("Method {} returns an observable, but not corresponding config method found with name {}",
                method.getName(), otherMethodName, ex);
            return MethodValidationState.OBSERVABLE_WITH_NO_MATCHING_CONFIG_METHOD;
        }

        if (Observable.class.isAssignableFrom(otherMethod.getReturnType())) {
            log.warn("Method {} returns an observable, but corresponding config method with name {} ALSO returns an observable!",
                method.getName(), otherMethodName);
            return MethodValidationState.OBSERVABLE_WITH_NO_MATCHING_CONFIG_METHOD;
        }

        // The other method will be validated independently.
        return MethodValidationState.OK;
    }

    /**
     * Validates that a given config interface is valid. NOTE that this does not validate the methods, which needs
     * to be done with separate calls to {@link #isValidConfigInterfaceMethod(java.lang.reflect.Method) }.
     *
     * @param <T>         the config interface class
     * @param configClass a reference to the config interface class
     * @return true if the config interface has passed validations
     */
    public static <T> boolean isValidConfigInterface(Class<T> configClass)
    {
        checkNotNull(configClass);
        if (!configClass.isInterface()) {
            log.warn("Class {} is not a valid config interface; it is not an interface.", configClass.getName());
            return false;
        }

        if (Arrays.stream(configClass.getMethods()).noneMatch(m -> !m.isDefault())) {
            log.warn("Class {} is not a valid config interface; it has no non-default methods defined.", configClass.getName());
            return false;
        }
        return true;
    }
}
