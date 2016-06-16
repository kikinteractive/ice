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
package com.kik.config.ice.exception;

import org.slf4j.helpers.MessageFormatter;

public class ConfigException extends RuntimeException
{
    public ConfigException(String msgFormat, Object... msgArgs)
    {
        super(
            MessageFormatter.arrayFormat(msgFormat, msgArgs).getMessage(),
            MessageFormatter.arrayFormat(msgFormat, msgArgs).getThrowable());
    }

    public ConfigException(Exception inner)
    {
        super(inner);
    }
}
