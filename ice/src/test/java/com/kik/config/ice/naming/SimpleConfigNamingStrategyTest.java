package com.kik.config.ice.naming;

import java.lang.reflect.Method;
import java.util.Optional;
import static junit.framework.Assert.assertEquals;
import org.junit.Test;

public class SimpleConfigNamingStrategyTest
{
    private final SimpleConfigNamingStrategy strat = new SimpleConfigNamingStrategy();

    public interface NameTest
    {
        int abc_def();

        long abcDef();

        boolean oneTwoThree();
    }

    @Test(timeout = 5000)
    public void testMethodNameConvert() throws Exception
    {
        Method m1 = NameTest.class.getMethod("abc_def");
        assertEquals("com.kik.config.ice.naming.SimpleConfigNamingStrategyTest$NameTest.abc_def",
            strat.methodToFlatName(m1, Optional.empty()));

        Method m2 = NameTest.class.getMethod("abcDef");
        assertEquals("com.kik.config.ice.naming.SimpleConfigNamingStrategyTest$NameTest.abcDef",
            strat.methodToFlatName(m2, Optional.empty()));

        Method m3 = NameTest.class.getMethod("oneTwoThree");
        assertEquals("com.kik.config.ice.naming.SimpleConfigNamingStrategyTest$NameTest.oneTwoThree",
            strat.methodToFlatName(m3, Optional.empty()));
    }

    @Test(timeout = 5000)
    public void testMethodNameConvert_withScopes() throws Exception
    {
        Method m1 = NameTest.class.getMethod("abc_def");
        assertEquals("com.kik.config.ice.naming.SimpleConfigNamingStrategyTest$NameTest.abc_def:scope1",
            strat.methodToFlatName(m1, Optional.of("scope1")));

        Method m2 = NameTest.class.getMethod("abcDef");
        assertEquals("com.kik.config.ice.naming.SimpleConfigNamingStrategyTest$NameTest.abcDef:scope2",
            strat.methodToFlatName(m2, Optional.of("scope2")));

        Method m3 = NameTest.class.getMethod("oneTwoThree");
        assertEquals("com.kik.config.ice.naming.SimpleConfigNamingStrategyTest$NameTest.oneTwoThree:",
            strat.methodToFlatName(m3, Optional.of("")));
    }
}
