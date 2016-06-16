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

import com.google.common.annotations.VisibleForTesting;
import static com.google.common.base.Preconditions.checkState;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.kik.config.ice.exception.ConfigException;
import com.kik.config.ice.internal.ConfigChangeEvent;
import com.kik.config.ice.internal.ConfigDescriptor;
import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;

@Slf4j
@Singleton
public class FileDynamicConfigSource extends AbstractDynamicConfigSource
{
    private static final int CONFIG_SOURCE_PRIORITY_DEFAULT = 100;

    private static final String FILENAME_DEFAULT = "./app.config";
    private static final Duration POLL_INTERVAL_DEFAULT = Duration.ZERO;
    private static final Pattern CONFIG_LINE_PATTERN = Pattern.compile("^\\s*(?!//|#)(?<key>\\S+?)\\s*=\\s*(?<value>.*?)\\s*$", Pattern.CASE_INSENSITIVE);

    private static final String CONFIG_PREFIX = "FileDynamicConfigSource.";

    /**
     * Name of optional binding for filename override.
     */
    public static final String FILENAME_NAME = CONFIG_PREFIX + "filename";
    /**
     * Name of Duration instance to be injected for a file poll interval
     */
    public static final String POLL_INTERVAL_NAME = CONFIG_PREFIX + "poll_interval";
    /**
     * Name of ScheduledExecutorService to be injected for
     */
    public static final String EXECUTOR_NAME = CONFIG_PREFIX + "executor";

    private File file;
    private ScheduledFuture<?> pollFuture;
    private volatile boolean closed;

    private volatile boolean isInitialized = false;
    private final Object initializationLock = new Object();

    @Inject(optional = true)
    @Named(FILENAME_NAME)
    private String filename;

    @Inject(optional = true)
    @Named(EXECUTOR_NAME)
    private ScheduledExecutorService executorService;

    @Inject(optional = true)
    @Named(POLL_INTERVAL_NAME)
    private Duration pollInterval;

    @Inject
    protected FileDynamicConfigSource(Set<ConfigDescriptor> configDescriptors)
    {
        super(configDescriptors);
    }

    @Inject
    protected void initializeIfNeeded()
    {
        if (!isInitialized) {
            synchronized (initializationLock) {
                initialize();
            }
        }
    }

    protected void initialize()
    {
        if (isInitialized) {
            return;
        }

        if (filename == null) {
            filename = FILENAME_DEFAULT;
        }

        log.debug("Config filename is \'{}\'.", filename);

        file = new File(filename);
        if (!file.isFile()) {
            throw new ConfigException("Config filename \'{}\' does not exist, or is not a file.", file.getAbsolutePath());
        }

        readFile();

        if (pollInterval == null) {
            pollInterval = POLL_INTERVAL_DEFAULT;
        }
        if (executorService == null) {
            executorService = Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder()
                .setNameFormat(FileDynamicConfigSource.class.getSimpleName() + "-Poll-%d")
                .build());
        }

        if (this.executorService != null && this.pollInterval != Duration.ZERO) {
            pollFuture = this.executorService.scheduleWithFixedDelay(this::readFile, this.pollInterval.toMillis(), this.pollInterval.toMillis(), TimeUnit.MILLISECONDS);
        }

        closed = false;
        isInitialized = true;
    }

    @Override
    public Observable<ConfigChangeEvent<String>> getObservable(String configName)
    {
        checkState(!closed);
        if (!subjectMap.containsKey(configName)) {
            throw new ConfigException("Unknown configName {}", configName);
        }
        return subjectMap.get(configName);
    }

    private void readFile()
    {
        try {
            log.debug("Reading config file now...");
            Set<String> remainingConfigKeys = Sets.newHashSet(subjectMap.keySet());

            Files.lines(file.toPath()).forEach(line -> {
                // parse line
                ConfigChangeEvent<String> event = parseLine(line);
                if (event == null) {
                    return;
                }

                log.trace("File-based event: {}", event);

                remainingConfigKeys.remove(event.getName());

                // update currentValues
                emitEvent(event);
            });

            // Remaining keys are effectively "Removed" - emit the removals if this is different from previous
            remainingConfigKeys.stream().forEach(key -> {
                emitEvent(key, Optional.empty());
            });
        }
        catch (Exception ex) {
            log.error("Error while reading config file {}", file, ex);
        }
    }

    @VisibleForTesting
    static ConfigChangeEvent<String> parseLine(String line)
    {
        if (Strings.isNullOrEmpty(line)) {
            return null;
        }
        Matcher matcher = CONFIG_LINE_PATTERN.matcher(line.trim());
        if (!matcher.matches()) {
            return null;
        }
        return new ConfigChangeEvent<>(matcher.group("key"), Optional.ofNullable(Strings.emptyToNull(matcher.group("value"))));
    }

    /**
     * Service is implemented as an inner class to get around multiple inheritance issue.
     */
    @Singleton
    private static class FileDynamicConfigSourceService extends AbstractIdleService
    {
        @Inject
        FileDynamicConfigSource configSource;

        @Override
        protected void startUp() throws Exception
        {
            // Initialization is handled in the injected initializer method so that configuration injection is not
            // dependent on ServiceManager startup
        }

        @Override
        protected void shutDown() throws Exception
        {
            // stop polling
            if (configSource.pollFuture != null) {
                configSource.pollFuture.cancel(true);
                configSource.pollFuture = null;
            }
            configSource.closed = true;

            configSource.subjectMap.values().forEach(subject -> subject.onCompleted());

            if (configSource.executorService != null) {
                configSource.executorService.shutdown();
            }
        }
    }

    public static Module module()
    {
        return module(CONFIG_SOURCE_PRIORITY_DEFAULT);
    }

    public static Module module(final int configSourcePriority)
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                MapBinder<Integer, DynamicConfigSource> mapBinder = MapBinder.newMapBinder(binder(), Integer.class, DynamicConfigSource.class);
                mapBinder.addBinding(configSourcePriority).to(FileDynamicConfigSource.class);

                // Bind inner class as a service to ensure resource cleanup
                Multibinder.newSetBinder(binder(), Service.class).addBinding().to(FileDynamicConfigSourceService.class);
            }
        };
    }
}
