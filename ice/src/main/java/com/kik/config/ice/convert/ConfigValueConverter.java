package com.kik.config.ice.convert;

import java.util.function.Function;

public interface ConfigValueConverter<T> extends Function<String, T>
{
}
