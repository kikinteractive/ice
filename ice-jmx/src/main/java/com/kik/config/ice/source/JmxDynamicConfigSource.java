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
package com.kik.config.ice.source;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.kik.config.ice.exception.ConfigException;
import com.kik.config.ice.internal.ConfigDescriptor;
import com.kik.config.ice.internal.ConfigDescriptorHolder;
import com.kik.config.ice.sink.ConfigEventSink;
import java.lang.ref.WeakReference;
import static java.util.Comparator.comparing;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import lombok.extern.slf4j.Slf4j;

/**
 * A {@link DynamicConfigSource} which registers all configuration as DynamicMBeans in JMX.
 * The configuration beans will be named based on the class which declares the config interface, and includes a
 * read/write attribute for each configuration method.
 * <br>
 * <b>NOTE:</b> In practice, the attributes in JMX will show the current value of the configuration value as the system
 * will be using it. If the config change fails, the value in JMX will revert again to the most recent value.
 * <br>
 * <b>NOTE:</b> Be aware, other config sources may change the value, and may not be represented in the JMX interface
 * until the attribute list has been refreshed.
 * <b>NOTE:</b> To undo a JMX change, clear the attribute field. This will remove any JMX-based override of the value,
 * and the value shown will be the new active value of the config system.
 */
@Slf4j
@Singleton
public class JmxDynamicConfigSource extends AbstractDynamicConfigSource implements ConfigEventSink<String>
{
    private static final int CONFIG_SOURCE_PRIORITY_DEFAULT = 25;

    private final WeakReference<Injector> injectorRef; // WeakReference to prevent the mbean server from leaking the injector
    private final MBeanServer mbeanServer;

    @Inject
    protected JmxDynamicConfigSource(Injector injector, MBeanServer mbeanServer, ConfigDescriptorHolder configDescriptorHolder)
    {
        super(configDescriptorHolder.configDescriptors);
        this.injectorRef = new WeakReference<>(injector);
        this.mbeanServer = mbeanServer;
        initializeJmxBeans();
    }

    private void initializeJmxBeans()
    {
        Map<String, List<ConfigDescriptor>> sortedDescriptors = configDescriptors.stream()
            .collect(groupingBy(desc -> desc.getMethod().getDeclaringClass().getName() + desc.getScope().orElse("")));
        sortedDescriptors.values().stream().forEach(list -> {
            list.sort(comparing(desc -> desc.getConfigName()));
        });

        // Construct JMX beans for each config class
        List<ConfigDynamicMBean> configBeans = sortedDescriptors.values().stream()
            .map(descList -> new ConfigDynamicMBean(this, injectorRef.get(), descList))
            .collect(toList());

        // Register to MBeanServer
        configBeans.stream().forEach(bean -> {
            try {
                mbeanServer.registerMBean(bean, new ObjectName(bean.getMBeanName()));
                log.debug("Registered bean with name {}", bean.getMBeanName());
            }
            catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException ex) {
                Optional<ConfigDescriptor> anyDesc = configDescriptors.stream().findAny();
                String msg = String.format("Failed to register MBean for class %s scope '%s'",
                    anyDesc.map(d -> d.getMethod().getDeclaringClass().getDeclaringClass().getName()).orElse("UNKNOWN"),
                    anyDesc.map(d -> d.getScope().orElse("")).orElse(""));
                log.warn(msg, ex);
                throw new ConfigException(msg, ex);
            }
        });
    }

    /**
     * Used by instances of {@link ConfigDynamicMBean} to emit values back to the config system.
     *
     * @param configName full configuration name from the descriptor
     * @param valueOpt   the value to be emitted (if different from last emission)
     */
    @Override
    public void fireEvent(String configName, Optional<String> valueOpt)
    {
        this.emitEvent(configName, valueOpt);
    }

    public static Module module()
    {
        return module(CONFIG_SOURCE_PRIORITY_DEFAULT);
    }

    public static Module module(final int priority)
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                MapBinder<Integer, DynamicConfigSource> mapBinder = MapBinder.newMapBinder(binder(), Integer.class, DynamicConfigSource.class);
                mapBinder.addBinding(priority).to(JmxDynamicConfigSource.class);
                bind(JmxDynamicConfigSource.class);
            }
        };
    }
}
