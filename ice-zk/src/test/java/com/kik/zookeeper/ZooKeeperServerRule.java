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
package com.kik.zookeeper;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.test.TestingServer;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;

/**
 * JUnit Test Rule intended for use with {@link ClassRule} or {@link Rule} although ClassRule is preferred due to
 * performance costs.
 */
@Slf4j
public class ZooKeeperServerRule extends ExternalResource
{
    private final List<Callback> initCallbacks;

    private TestingServer testingServer;

    public ZooKeeperServerRule()
    {
        this.initCallbacks = Lists.newArrayList();
    }

    @Override
    protected void before() throws Throwable
    {
        assertNull(testingServer);
        System.setProperty("zookeeper.forceSync", "no"); // Just in memory!
        testingServer = new TestingServer();

        log.trace("Calling {} initilization callbacks", initCallbacks.size());
        for (Callback c : initCallbacks) {
            c.onInit(this);
        }
    }

    @Override
    protected void after()
    {
        try {
            if (testingServer != null) {
                testingServer.close();
            }
        }
        catch (Throwable ex) {
            log.warn("Error closing test server", ex);
        }
        finally {
            testingServer = null;
            initCallbacks.clear();
        }
    }

    public void registerOnInitCallback(Callback callback)
    {
        assertNull(testingServer);
        if (callback != null && !initCallbacks.contains(callback)) {
            initCallbacks.add(callback);
        }
    }

    public boolean initiated()
    {
        return testingServer != null;
    }

    public String getConnectionString()
    {
        assertNotNull(testingServer);
        return testingServer.getConnectString();
    }

    public interface Callback
    {
        void onInit(ZooKeeperServerRule rule);
    }
}
