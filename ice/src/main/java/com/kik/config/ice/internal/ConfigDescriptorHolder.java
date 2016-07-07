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
package com.kik.config.ice.internal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;

/**
 * ConfigDescriptorHolder is a utility class that allows for a set
 * of {@link ConfigDescriptor} to be optionally bound.
 *
 * This is useful in cases where the set of config descriptors is
 * required in a constructor, where dependencies cannot be optionally
 * bound. Example:
 *
 * {@literal @}Inject
 * protected MyConstructor(ConfigDescriptorHolder configDescriptorHolder)
 * {
 *     // configDescriptorHolder is required to be bound,
 *     // but configDescriptorHolder.configDescriptors is optional.
 * }
 *
 */
@Singleton
public class ConfigDescriptorHolder
{
    @Inject(optional = true)
    public Set<ConfigDescriptor> configDescriptors;
}
