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
package com.kik.config.ice.convert;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.util.Types;
import com.kik.config.ice.internal.ConfigBuilder;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defines a module which binds standard {@link ConfigValueConverter} definitions for use with {@link ConfigBuilder}.
 */
public class ConfigValueConverters
{
    private static final Pattern CSV_PATTERN = Pattern.compile("(^|,)(?:"
        + "(?<notQuoted>[^,\"]*)"
        + "|"
        + "\\s*\"(?<quoted>(?:[^\"]|\"\")*)\"\\s*"
        + ")(?=,|$)");

    public static String identity(String input)
    {
        return input;
    }

    public static Double toDouble(String input)
    {
        if (Strings.isNullOrEmpty(input)) {
            return null;
        }
        return Double.parseDouble(input);
    }

    public static Long toLong(String input)
    {
        if (Strings.isNullOrEmpty(input)) {
            return null;
        }
        return Long.parseLong(input);
    }

    public static Integer toInteger(String input)
    {
        if (Strings.isNullOrEmpty(input)) {
            return null;
        }
        return Integer.parseInt(input);
    }

    public static Boolean toBoolean(String input)
    {
        if (Strings.isNullOrEmpty(input)) {
            return null;
        }
        return Boolean.parseBoolean(input);
    }

    public static Duration toDuration(String input)
    {
        if (Strings.isNullOrEmpty(input)) {
            return null;
        }
        return Duration.parse(input);
    }

    public static LocalTime toLocalTime(String input)
    {
        if (Strings.isNullOrEmpty(input)) {
            return null;
        }
        return LocalTime.parse(input);
    }

    public static LocalDate toLocalDate(String input)
    {
        if (Strings.isNullOrEmpty(input)) {
            return null;
        }
        return LocalDate.parse(input);
    }

    public static Instant toInstant(String input)
    {
        if (Strings.isNullOrEmpty(input)) {
            return null;
        }
        return Instant.parse(input);
    }

    public static ZonedDateTime toZonedDateTime(String input)
    {
        if (Strings.isNullOrEmpty(input)) {
            return null;
        }
        return ZonedDateTime.parse(input);
    }

    @VisibleForTesting
    static List<String> parseCsvLine(final String input)
    {
        if (input == null) {
            return Collections.emptyList();
        }
        Matcher m = CSV_PATTERN.matcher(input);
        List<String> result = Lists.newArrayList();
        while (m.find()) {
            final String notQuoted = m.group("notQuoted");
            final String quoted = m.group("quoted");
            if (quoted == null) {
                result.add(notQuoted);
            }
            else {
                result.add(quoted.replaceAll("\"\"", "\""));
            }
        }
        return result;
    }

    public static List<String> toStringList(String input)
    {
        if (Strings.isNullOrEmpty(input)) {
            return null;
        }
        return parseCsvLine(input);
    }

    public static Set<String> toStringSet(String input)
    {
        if (Strings.isNullOrEmpty(input)) {
            return null;
        }
        return ImmutableSet.copyOf(parseCsvLine(input));
    }

    public static <T> Optional<T> toOptional(ConfigValueConverter<T> innerConverter, String input)
    {
        return Optional.ofNullable(innerConverter.apply(input));
    }

    public static Module module()
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                TypeLiteral<TypeLiteral<?>> typeType = new TypeLiteral<TypeLiteral<?>>()
                {
                };
                TypeLiteral<ConfigValueConverter<?>> converterType = new TypeLiteral<ConfigValueConverter<?>>()
                {
                };

                MapBinder<TypeLiteral<?>, ConfigValueConverter<?>> mapBinder = MapBinder.newMapBinder(binder(), typeType, converterType);

                bindConverter(String.class, mapBinder, ConfigValueConverters::identity);

                bindOptionalConverter(new TypeLiteral<Optional<String>>()
                {
                }, String.class, mapBinder, v -> toOptional(ConfigValueConverters::identity, v));

                bindConverter(Double.class, mapBinder, ConfigValueConverters::toDouble);
                bindOptionalConverter(new TypeLiteral<Optional<Double>>()
                {
                }, Double.class, mapBinder, v -> toOptional(ConfigValueConverters::toDouble, v));

                bindConverter(Long.class, mapBinder, ConfigValueConverters::toLong);
                bindOptionalConverter(new TypeLiteral<Optional<Long>>()
                {
                }, Long.class, mapBinder, v -> toOptional(ConfigValueConverters::toLong, v));
                bindConverter(Integer.class, mapBinder, ConfigValueConverters::toInteger);
                bindOptionalConverter(new TypeLiteral<Optional<Integer>>()
                {
                }, Integer.class, mapBinder, v -> toOptional(ConfigValueConverters::toInteger, v));
                bindConverter(Boolean.class, mapBinder, ConfigValueConverters::toBoolean);
                bindOptionalConverter(new TypeLiteral<Optional<Boolean>>()
                {
                }, Boolean.class, mapBinder, v -> toOptional(ConfigValueConverters::toBoolean, v));
                bindConverter(Duration.class, mapBinder, ConfigValueConverters::toDuration);
                bindOptionalConverter(new TypeLiteral<Optional<Duration>>()
                {
                }, Duration.class, mapBinder, v -> toOptional(ConfigValueConverters::toDuration, v));

                bindConverter(LocalTime.class, mapBinder, ConfigValueConverters::toLocalTime);
                bindOptionalConverter(new TypeLiteral<Optional<LocalTime>>()
                {
                }, LocalTime.class, mapBinder, v -> toOptional(ConfigValueConverters::toLocalTime, v));

                bindConverter(LocalDate.class, mapBinder, ConfigValueConverters::toLocalDate);
                bindOptionalConverter(new TypeLiteral<Optional<LocalDate>>()
                {
                }, LocalDate.class, mapBinder, v -> toOptional(ConfigValueConverters::toLocalDate, v));

                bindConverter(Instant.class, mapBinder, ConfigValueConverters::toInstant);
                bindOptionalConverter(new TypeLiteral<Optional<Instant>>()
                {
                }, Instant.class, mapBinder, v -> toOptional(ConfigValueConverters::toInstant, v));

                bindConverter(ZonedDateTime.class, mapBinder, ConfigValueConverters::toZonedDateTime);
                bindOptionalConverter(new TypeLiteral<Optional<ZonedDateTime>>()
                {
                }, ZonedDateTime.class, mapBinder, v -> toOptional(ConfigValueConverters::toZonedDateTime, v));

                final TypeLiteral<List<String>> listOfStringType = new TypeLiteral<List<String>>()
                {
                };
                bindConverter(listOfStringType, mapBinder, ConfigValueConverters::toStringList);

                final TypeLiteral<Set<String>> setOfStringType = new TypeLiteral<Set<String>>()
                {
                };
                bindConverter(setOfStringType, mapBinder, ConfigValueConverters::toStringSet);
            }

            private <T> void bindConverter(
                Class<T> convertToType,
                MapBinder<TypeLiteral<?>, ConfigValueConverter<?>> mapBinder,
                ConfigValueConverter<T> converterInstance)
            {
                // Bind into map binder
                mapBinder.addBinding(TypeLiteral.get(convertToType)).toInstance(converterInstance);

                // Bind for individual injection
                bind((TypeLiteral<ConfigValueConverter<T>>) TypeLiteral.get(Types.newParameterizedType(ConfigValueConverter.class, convertToType))).toInstance(converterInstance);
            }

            private <T, I> void bindOptionalConverter(
                TypeLiteral<T> convertToType,
                Class<I> innerConverterType,
                MapBinder<TypeLiteral<?>, ConfigValueConverter<?>> mapBinder,
                ConfigValueConverter<T> converterInstance)
            {
                // Bind into map binder
                mapBinder.addBinding(convertToType).toInstance(converterInstance);

                // Bind for individual injection
                bind((TypeLiteral<ConfigValueConverter<T>>) TypeLiteral.get(
                    Types.newParameterizedType(ConfigValueConverter.class,
                        Types.newParameterizedType(Optional.class, innerConverterType)))).toInstance(converterInstance);
            }

            private <T> void bindConverter(
                TypeLiteral<T> convertToType,
                MapBinder<TypeLiteral<?>, ConfigValueConverter<?>> mapBinder,
                ConfigValueConverter<T> converterInstance)
            {
                // Bind into map binder
                mapBinder.addBinding(convertToType).toInstance(converterInstance);

                // Bind for individual injection
                bind((TypeLiteral<ConfigValueConverter<T>>) TypeLiteral.get(Types.newParameterizedType(ConfigValueConverter.class, convertToType.getType()))).toInstance(converterInstance);
            }
        };
    }
}
