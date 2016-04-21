package com.kik.config.ice.internal;

import ch.qos.logback.classic.Level;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.NoDefaultValue;
import com.kik.config.ice.annotations.DefaultValue;
import com.kik.config.ice.exception.ConfigException;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import rx.Observable;

public class ConfigValidationTest
{
    public static void setLogLevel(Class<?> loggingClass, Level logLevel)
    {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggingClass);
        logger.setLevel(logLevel);
    }

    static {
        setLogLevel(ConfigBuilder.class, Level.TRACE);
        setLogLevel(StaticConfigHelper.class, Level.TRACE);
    }

    //<editor-fold defaultstate="collapsed" desc="Various test config interfaces">
    public interface NoMethodsConfig
    {
    }

    public interface OnlyDefaultMethodsConfig
    {
        default String foo()
        {
            return "abc";
        }
    }

    public static class NotAnInterfaceConfig
    {
    }

    public interface MissingDefaultConfig
    {
        String foo();
    }

    public interface HasParamsConfig
    {
        @DefaultValue("abc")
        String foo(int blah);
    }

    public interface AmbiguousDefaultConfig
    {
        @DefaultValue("abc")
        @NoDefaultValue
        String foo();
    }

    public interface GoodConfigWithNoDefault
    {
        @NoDefaultValue
        String foo();
    }

    public interface GoodConfigWithDefault
    {
        @DefaultValue("abc")
        String foo();
    }

    public interface ObservableWithBadName
    {
        @DefaultValue("foo")
        String foo();

        Observable<String> foo2();
    }

    public interface ObservableWithoutCorrespondingConfig
    {
        @DefaultValue("foo")
        String foo();

        // needs but is missing "bar" config method
        Observable<String> barObservable();
    }

    public interface ObservableCorrespondingToObservable
    {
        @DefaultValue("foo")
        String foo();

        Observable<String> fooObservable();

        Observable<Observable<String>> fooObservableObservable(); // bad
    }

    public interface PrimitivesWithDefaults
    {
        @DefaultValue("123")
        int foo();

        @DefaultValue("true")
        boolean bar();
    }

    @NoDefaultValue
    public interface PrimitiveWithoutDefaults
    {
        int foo();

        boolean bar();
    }
    //</editor-fold>

    @Test(timeout = 1000, expected = ConfigException.class)
    public void testNoMethods() throws Exception
    {
        ConfigSystem.configModule(NoMethodsConfig.class);
    }

    @Test(timeout = 1000, expected = ConfigException.class)
    public void testOnlyDefaultMethods() throws Exception
    {
        ConfigSystem.configModule(OnlyDefaultMethodsConfig.class);
    }

    @Test(timeout = 1000, expected = ConfigException.class)
    public void testMissingDefaults() throws Exception
    {
        ConfigSystem.configModule(MissingDefaultConfig.class);
    }

    @Test(timeout = 1000, expected = ConfigException.class)
    public void testNotAnInterface() throws Exception
    {
        ConfigSystem.configModule(NotAnInterfaceConfig.class);
    }

    @Test(timeout = 1000, expected = ConfigException.class)
    public void testHasParams() throws Exception
    {
        ConfigSystem.configModule(HasParamsConfig.class);
    }

    @Test(timeout = 1000, expected = ConfigException.class)
    public void testAmbiguousDefault() throws Exception
    {
        ConfigSystem.configModule(AmbiguousDefaultConfig.class);
    }

    @Test(timeout = 1000)
    public void testGoodConfigs()
    {
        ConfigSystem.configModule(GoodConfigWithNoDefault.class);
        ConfigSystem.configModule(GoodConfigWithDefault.class);
    }

    @Test(timeout = 1000, expected = ConfigException.class)
    public void testObservableWithBadName()
    {
        ConfigSystem.configModule(ObservableWithBadName.class);
    }

    @Test(timeout = 1000, expected = ConfigException.class)
    public void testObservableWithoutCorrespondingConfig()
    {
        ConfigSystem.configModule(ObservableWithoutCorrespondingConfig.class);
    }

    @Test(timeout = 1000, expected = ConfigException.class)
    public void testObservableCorrespondingToObservable()
    {
        ConfigSystem.configModule(ObservableCorrespondingToObservable.class);
    }

    @Test(timeout = 1000)
    public void testPrimitivesWithDefaults()
    {
        ConfigSystem.configModule(PrimitivesWithDefaults.class);
    }

    @Test(timeout = 1000, expected = ConfigException.class)
    public void testPrimitiveWithoutDefaults()
    {
        ConfigSystem.configModule(PrimitiveWithoutDefaults.class);
    }
}
