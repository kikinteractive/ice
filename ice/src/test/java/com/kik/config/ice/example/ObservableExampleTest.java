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
package com.kik.config.ice.example;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kik.config.ice.ConfigConfigurator;
import com.kik.config.ice.internal.ConfigBuilder;
import com.kik.config.ice.internal.ConstantValuePropertyAccessor;
import com.kik.config.ice.internal.OverrideModule;
import com.kik.config.ice.internal.PropertyAccessor;
import com.kik.config.ice.source.DebugDynamicConfigSource;
import com.kik.config.ice.source.FileDynamicConfigSource;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;

@Slf4j
public class ObservableExampleTest
{
    private static final String cfgName1 = "com.kik.config.ice.example.ObservableExample$Config.enabled";
    private static final String cfgName2 = "com.kik.config.ice.example.ObservableExample$Config.maxSize";

    public static void setLogLevel(Class<?> loggingClass, Level logLevel)
    {
        Logger logger = (Logger) LoggerFactory.getLogger(loggingClass);
        logger.setLevel(logLevel);
    }

    static {
        setLogLevel(DebugDynamicConfigSource.class, Level.TRACE);
        setLogLevel(FileDynamicConfigSource.class, Level.TRACE);
        setLogLevel(ConfigBuilder.class, Level.TRACE);
        setLogLevel(OverrideModule.class, Level.TRACE);
        setLogLevel(PropertyAccessor.class, Level.TRACE);
        setLogLevel(ConstantValuePropertyAccessor.class, Level.TRACE);
    }

    @Inject
    ObservableExample example;

    @Inject
    DebugDynamicConfigSource debugSource;

    @Before
    public void setup()
    {
        Injector injector = Guice.createInjector(
            ConfigConfigurator.testModules(),
            ObservableExample.module()
        );
        injector.injectMembers(this);
    }

    @Test(timeout = 5000)
    @SuppressWarnings("UnnecessaryUnboxing")
    public void testObservableMethods()
    {
        assertEquals(Boolean.TRUE, example.config.enabled());
        assertEquals(100, example.config.maxSize().intValue());

        Observable<Boolean> enabledObs = example.config.enabledObservable();
        Boolean value = enabledObs.toBlocking().first();
        assertEquals(Boolean.TRUE, value);

        ActionImpl<Boolean> boolAction = new ActionImpl<>("boolAction");
        enabledObs.subscribe(boolAction);
        assertEquals(Boolean.TRUE, boolAction.getValue());

        debugSource.fireEvent(cfgName1, Optional.of("false"));
        assertEquals(Boolean.FALSE, boolAction.getValue());

        Observable<Integer> intObs = example.config.maxSizeObservable();
        Integer intValue = intObs.toBlocking().first();
        assertEquals(100, intValue.intValue());

        ActionImpl<Integer> intAction = new ActionImpl<>("intAction");
        intObs.subscribe(intAction);
        assertEquals(100, intAction.getValue().intValue());

        debugSource.fireEvent(cfgName2, Optional.of("123"));
        assertEquals(123, intAction.getValue().intValue());

        debugSource.fireEvent(cfgName2, Optional.of("1000000"));
        assertEquals(1_000_000, intAction.getValue().intValue());
    }

    private static class ActionImpl<T> implements Action1<T>
    {
        private final String name;
        private T value;

        public ActionImpl(String name)
        {
            this.name = name;
            this.value = null;
        }

        @Override
        public void call(T newValue)
        {
            value = newValue;
            log.debug("Impl {} called with newValue {}", name, newValue);
        }

        public T getValue()
        {
            return value;
        }
    }
}
