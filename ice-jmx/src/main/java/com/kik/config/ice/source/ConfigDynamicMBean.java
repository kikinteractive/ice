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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.Maps;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.convert.ConfigValueConverter;
import com.kik.config.ice.internal.ConfigDescriptor;
import com.kik.config.ice.internal.PropertyAccessor;
import com.kik.config.ice.sink.ConfigEventSink;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import static java.util.Comparator.comparing;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static java.util.stream.Collectors.joining;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;
import lombok.extern.slf4j.Slf4j;

/**
 * Bean class used by {@link JmxDynamicConfigSource} to register configuration in JMX.
 */
@Slf4j
public class ConfigDynamicMBean implements DynamicMBean
{
    private final static String MBEAN_SUFFIX = "IceMBean";
    private final WeakReference<Injector> injectorRef;
    private final ConfigEventSink<String> eventSink;
    private final Map<String, Provider<PropertyAccessor<?>>> providerLookupByAttributeName;
    private final Map<String, ConfigDescriptor> descLookupByAttributeName;

    private final String mbeanName;
    private final MBeanInfo mbeanInfo;

    public ConfigDynamicMBean(ConfigEventSink<String> eventSink, Injector injector, List<ConfigDescriptor> configDescriptors)
    {
        checkNotNull(eventSink);
        checkNotNull(injector);
        checkNotNull(configDescriptors);
        checkArgument(!configDescriptors.isEmpty());

        this.mbeanName = descToBeanName(configDescriptors.get(0));
        this.eventSink = eventSink;
        this.injectorRef = new WeakReference<>(injector);
        this.providerLookupByAttributeName = Maps.newHashMap();
        this.descLookupByAttributeName = Maps.newHashMap();
        for (ConfigDescriptor desc : configDescriptors) {
            String attrName = desc.getMethod().getName();
            log.trace("MBean {} Found Attribute {}", mbeanName, attrName);

            // Add entry to descriptor lookup map
            this.descLookupByAttributeName.put(attrName, desc);

            // Add entry to provider lookup map
            TypeLiteral<PropertyAccessor<?>> accessorKey = (TypeLiteral<PropertyAccessor<?>>) TypeLiteral.get(Types.newParameterizedType(PropertyAccessor.class, desc.getConfigType()));
            this.providerLookupByAttributeName.put(attrName, injectorRef.get().getProvider(Key.get(accessorKey, ConfigSystem.getIdentifier(desc))));
        }

        MBeanAttributeInfo[] attributeInfos = configDescriptors.stream()
            .sorted(comparing(d -> d.getConfigName()))
            .map(ConfigDynamicMBean::descToAttributeInfo)
            .toArray(MBeanAttributeInfo[]::new);

        String className = configDescriptors.get(0).getMethod().getDeclaringClass().getDeclaringClass().getName();
        this.mbeanInfo = new MBeanInfo(className, "", attributeInfos, null, null, null);
    }

    private static MBeanAttributeInfo descToAttributeInfo(ConfigDescriptor desc)
    {
        return new MBeanAttributeInfo(
            desc.getMethod().getName(),
            "java.lang.String", // Always use string, or JMX will try to parse things for us.
            "Actual Type: " + desc.getConfigType().getTypeName(), // description - TODO: get from an annotation?
            true, // readable
            true, // writable
            false /* isIs */);
    }

    static String descToBeanName(ConfigDescriptor desc)
    {
        // class which is declaring the config interface
        final Class<?> declaringClass = desc.getMethod().getDeclaringClass().getDeclaringClass();

        final String jmxDomain = declaringClass.getPackage().getName() + ":";
        final String jmxName = "name=" + declaringClass.getSimpleName() + MBEAN_SUFFIX;
        final String jmxScope = desc.getScope().map(s -> ",scope=" + s).orElse("");
        return jmxDomain + jmxName + jmxScope;
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException
    {
        Provider<PropertyAccessor<?>> propertyAccessorProvider = providerLookupByAttributeName.get(attribute);
        if (propertyAccessorProvider == null) {
            log.warn("Attribute {} requested on MXBean {}, but associated propertyAccessorProvider not found.", attribute, mbeanName);
            throw new AttributeNotFoundException();
        }
        Object value = propertyAccessorProvider.get().get();
        if (value instanceof Optional) {
            value = ((Optional) value).orElse(null);
        }
        else if (value instanceof List) {
            // TODO - Bit of a hack - need to revisit how to do this.
            value = ((List) value).stream().map(String::valueOf).collect(joining(","));
        }
        log.trace("Attribute {} requested on MXBean {}, returning value '{}'", attribute, mbeanName, value);
        return value;
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException
    {
        ConfigDescriptor desc = descLookupByAttributeName.get(attribute.getName());
        if (desc == null) {
            log.warn("Attribute Set name={} value={} requested on MXBean {}, but no matching configDescriptor found.",
                attribute.getName(), attribute.getValue(), mbeanName);
            throw new AttributeNotFoundException();
        }

        // get appropriate config value converter and test the conversion for immediate error feedback
        ConfigValueConverter<?> converter = (ConfigValueConverter<?>) this.injectorRef.get().getInstance(Key.get(Types.newParameterizedType(ConfigValueConverter.class, desc.getConfigType())));
        String strValue = attribute.getValue() == null ? null : attribute.getValue().toString();
        try {
            converter.apply(strValue);
        }
        catch (Exception ex) {
            log.warn("Attribute Set name={} value={} requested on MXBean {}, but value failed to convert to type {}",
                attribute.getName(), attribute.getValue(), mbeanName, desc.getConfigType().getTypeName());
            throw new InvalidAttributeValueException("Failed to parse value: " + ex.getMessage());
        }

        // emit event
        this.eventSink.fireEvent(desc.getConfigName(), Optional.ofNullable(strValue));
    }

    @Override
    public AttributeList getAttributes(String[] attributes)
    {
        AttributeList lst = new AttributeList(attributes.length);
        for (String attribute : attributes) {
            try {
                lst.add(new Attribute(attribute, getAttribute(attribute)));
            }
            catch (AttributeNotFoundException | MBeanException | ReflectionException ex) {
                // warn is logged by individual getAttribute calls
                log.debug("getAttributes was unable to fetch attribute {} from MXBean {}", attribute, this.mbeanName, ex);
            }
        }
        return lst;
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes)
    {
        List<String> toGet = new ArrayList<>();
        for (Attribute attrib : attributes.asList()) {
            try {
                setAttribute(attrib);
                toGet.add(attrib.getName());
            }
            catch (AttributeNotFoundException | InvalidAttributeValueException | MBeanException | ReflectionException ex) {
                // warn is logged by individual setAttribute calls
                log.debug("Could not set attribute {} on MXBean {}", attrib.getName(), mbeanName, ex);
            }
        }
        return getAttributes(toGet.stream().toArray(String[]::new));
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException
    {
        log.warn("Unexpected invoke for action {} on MXBean {}", actionName, mbeanName);
        throw new MBeanException(
            new UnsupportedOperationException("methods not implemented."));
    }

    @Override
    public MBeanInfo getMBeanInfo()
    {
        return this.mbeanInfo;
    }

    public String getMBeanName()
    {
        return this.mbeanName;
    }
}
