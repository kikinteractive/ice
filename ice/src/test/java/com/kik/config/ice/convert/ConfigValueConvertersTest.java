package com.kik.config.ice.convert;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public class ConfigValueConvertersTest
{
    @Inject
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private Map<TypeLiteral<?>, ConfigValueConverter<?>> converterMap;

    @Inject
    private ConfigValueConverter<List<String>> stringListConverter;

    @Inject
    private ConfigValueConverter<Optional<Integer>> optIntConverter;

    @Before
    public void init()
    {
        Injector injector = Guice.createInjector(ConfigValueConverters.module());
        injector.injectMembers(this);
    }

    @Test(timeout = 1000)
    public void testConverter_doesNotExist()
    {
        // Asking for a type that isn't in the map
        ConfigValueConverter<?> converter = converterMap.get(TypeLiteral.get(ConfigValueConvertersTest.class));
        assertNull(converter);
    }

    @Test(timeout = 5000)
    public void testConverters_int()
    {
        ConfigValueConverter<Integer> converter = getConverter(Integer.class);
        Integer value = converter.apply("321");
        assertEquals(321, value.intValue());
    }

    @Test(timeout = 5000)
    public void testConverters_long()
    {
        ConfigValueConverter<Long> converter = getConverter(Long.class);
        Long value = converter.apply("321");
        assertEquals(321L, (long) value);
    }

    @Test(timeout = 5000)
    public void testConverters_double()
    {
        ConfigValueConverter<Double> converter = getConverter(Double.class);
        assertEquals(1.23d, converter.apply("1.23"));
    }

    @Test(timeout = 5000)
    public void testConverters_duration()
    {
        ConfigValueConverter<Duration> converter = getConverter(Duration.class);
        assertEquals(Duration.ofHours(1), converter.apply("PT1H"));
    }

    @Test(timeout = 5000)
    public void testStringListConverter()
    {
        List<String> result = stringListConverter.apply(null);
        assertNull(result);

        result = stringListConverter.apply("");
        assertNull(result);

        result = stringListConverter.apply("a,b,c");
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test(timeout = 5000)
    public void testStringListConverter_viaMap()
    {
        ConfigValueConverter<List<String>> listConverter = (ConfigValueConverter<List<String>>) converterMap.get(TypeLiteral.get(Types.newParameterizedType(List.class, String.class)));
        List<String> result = listConverter.apply("abc,def,123");
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("abc", result.get(0));
        assertEquals("def", result.get(1));
        assertEquals("123", result.get(2));
    }

    @Test(timeout = 5000, expected = NumberFormatException.class)
    public void testConverter_failure()
    {
        ConfigValueConverter<Double> converter = getConverter(Double.class);
        converter.apply("abcd");
    }

    @Test(timeout = 5000)
    public void testOptionalIntegerConverter()
    {
        assertEquals(Optional.empty(), optIntConverter.apply(null));
        assertEquals(Optional.empty(), optIntConverter.apply(""));
        assertEquals(Optional.of(123), optIntConverter.apply("123"));
    }

    @Test(timeout = 5000, expected = NumberFormatException.class)
    public void testOptionalIntegerConverter_failure()
    {
        optIntConverter.apply("abcd");
    }

    @Test(timeout = 5000)
    public void testCsvParser()
    {
        assertEquals(0, ConfigValueConverters.parseCsvLine(null).size());
        assertListValues(ConfigValueConverters.parseCsvLine(""), "");
        assertListValues(ConfigValueConverters.parseCsvLine("a,b,c,"), "a", "b", "c", "");
        assertListValues(ConfigValueConverters.parseCsvLine("a,\"b\",c"), "a", "b", "c");
        assertListValues(ConfigValueConverters.parseCsvLine("a,\"b,b,b\",c"), "a", "b,b,b", "c");
        assertListValues(ConfigValueConverters.parseCsvLine("a, \"b\"\"stuff\"\"b\" ,c"), "a", "b\"stuff\"b", "c");
        assertListValues(ConfigValueConverters.parseCsvLine("\"a,b\"\"asdf\"\"b,c\""), "a,b\"asdf\"b,c");
    }

    @Test(timeout = 5000)
    public void testTimes()
    {
        ConfigValueConverter<LocalTime> timeConverter = getConverter(LocalTime.class);
        assertEquals(LocalTime.of(1, 2, 3, 4), timeConverter.apply("01:02:03.000000004"));

        ConfigValueConverter<LocalDate> dateConverter = getConverter(LocalDate.class);
        assertEquals(LocalDate.of(2015, Month.DECEMBER, 9), dateConverter.apply("2015-12-09"));

        ConfigValueConverter<ZonedDateTime> dateTimeConverter = getConverter(ZonedDateTime.class);
        assertEquals(ZonedDateTime.of(2015, 12, 9, 10, 32, 0, 0, ZoneId.of("Z")), dateTimeConverter.apply("2015-12-09T10:32:00Z"));

        ConfigValueConverter<Instant> instantConverter = getConverter(Instant.class);
        ZonedDateTime dtNow = ZonedDateTime.now();
        String instantString = dtNow.toInstant().toString();
        assertEquals(dtNow.toInstant(), instantConverter.apply(instantString));
    }

    private <T> void assertListValues(List<T> list, T... values)
    {
        for (int idx = 0; idx < list.size(); ++idx) {
            assertEquals("List index " + idx + " was not equal.", values[idx], list.get(idx));
        }
    }

    private <T> ConfigValueConverter<T> getConverter(Class<T> valueType)
    {
        assertNotNull(converterMap);
        ConfigValueConverter<?> converter = converterMap.get(TypeLiteral.get(valueType));
        assertNotNull(converter);
        return (ConfigValueConverter<T>) converter;
    }
}
