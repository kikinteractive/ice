package com.kik.config.ice.naming;

import java.lang.reflect.Method;
import java.util.Optional;

public interface ConfigNamingStrategy
{
    String methodToFlatName(Method method, Optional<String> scopeOpt);
}
