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
package com.kik.config.ice;

import com.google.inject.AbstractModule;

/**
 * ICE should work when the Guice Binder has requireExplicitBindings() enabled.
 * This module is added to the various tests to turn it on.
 */
public class ExplicitBindingModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        binder().requireExplicitBindings();
    }
}
