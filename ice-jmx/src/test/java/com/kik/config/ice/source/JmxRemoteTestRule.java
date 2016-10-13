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
import com.google.inject.Module;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

/**
 * NOTE: This class is not set up as a class rule due to the injection taking place.
 */
public class JmxRemoteTestRule
{
    @FunctionalInterface
    public static interface Test
    {
        public void test(MBeanServerConnection mbsc) throws Exception;
    }

    @Inject
    private MBeanServer mbs;

    private JMXServiceURL serviceUrl;
    private JMXConnectorServer connectorServer;
    private JMXConnector connector;
    private JMXServiceURL addr;
    private MBeanServerConnection mbsc;

    public void before() throws Exception
    {
        serviceUrl = new JMXServiceURL("service:jmx:rmi://");
        connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(serviceUrl, null, mbs);
        connectorServer.start();
        addr = connectorServer.getAddress();
        connector = JMXConnectorFactory.connect(addr);
        mbsc = connector.getMBeanServerConnection();
    }

    public void after() throws Exception
    {
        if (connector != null) {
            connector.close();
        }
        connectorServer.stop();
    }

    public void remoteTest(Test test) throws Exception
    {
        test.test(mbsc);
    }

    public static Module module()
    {
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind(JmxRemoteTestRule.class);
            }
        };
    }
}
