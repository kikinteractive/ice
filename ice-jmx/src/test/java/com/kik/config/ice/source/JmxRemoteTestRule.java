package com.kik.config.ice.source;

import com.google.inject.Inject;
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
}
