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
