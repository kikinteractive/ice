package com.kik.config.ice.source;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import static com.google.inject.name.Names.named;
import com.google.inject.util.Modules;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.annotations.NoDefaultValue;
import com.kik.config.ice.internal.ConfigChangeEvent;
import com.kik.config.ice.internal.ConfigDescriptor;
import com.kik.config.ice.internal.ConfigDescriptorFactory;
import com.kik.config.ice.naming.ConfigNamingStrategy;
import com.kik.config.ice.naming.SimpleConfigNamingStrategy;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import rx.Observable;

public class FileDynamicConfigSourceTest
{
    private static final ConfigNamingStrategy namingStrategy = new SimpleConfigNamingStrategy();
    private static final ConfigDescriptorFactory descriptorFactory = new ConfigDescriptorFactory(namingStrategy);

    @NoDefaultValue
    public interface Config
    {
        Boolean enabled();

        Integer abcDef();

        String notInFile();
    }

    @Inject
    FileDynamicConfigSource source;

    @Test(timeout = 5000)
    public void testFile() throws Exception
    {
        Injector injector = Guice.createInjector(Modules.override(
            ConfigConfigurator.standardModules())
            .with(new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    bind(Duration.class).annotatedWith(named(FileDynamicConfigSource.POLL_INTERVAL_NAME)).toInstance(Duration.ofMillis(250));
                    bind(String.class).annotatedWith(named(FileDynamicConfigSource.FILENAME_NAME)).toInstance(getClass().getResource("test.config").getFile());

                    final List<ConfigDescriptor> configDescList = descriptorFactory.buildDescriptors(Config.class, Optional.empty());
                    Multibinder<ConfigDescriptor> multiBinder = Multibinder.newSetBinder(binder(), ConfigDescriptor.class);
                    configDescList.stream().forEach(desc -> multiBinder.addBinding().toInstance(desc));
                }
            }));

        injector.injectMembers(this);

        assertNotNull(source);

        assertEquals(Optional.of("true"), getValueFor(Config.class.getMethod("enabled")));
        assertEquals(Optional.of("1234"), getValueFor(Config.class.getMethod("abcDef")));
        assertEquals(Optional.empty(), getValueFor(Config.class.getMethod("notInFile")));
    }

    private Optional<String> getValueFor(Method m)
    {
        String cfgName = namingStrategy.methodToFlatName(m, Optional.empty());
        Observable<ConfigChangeEvent<String>> obs = source.getObservable(cfgName);
        ConfigChangeEvent<String> event = obs.toBlocking().first();
        assertNotNull(event);
        return event.getValueOpt();
    }
}
