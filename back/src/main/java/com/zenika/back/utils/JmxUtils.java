package com.zenika.back.utils;

import com.zenika.back.model.ObjectNameRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JmxUtils {

    private static final Logger logger = LoggerFactory.getLogger(JmxUtils.class);

    public static List<ObjectNameRepresentation> objectNames(String host, int port) {
        String url = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port
                + "/jmxrmi";
        JMXServiceURL serviceURL = null;
        JMXConnector jmxConnector = null;

        try {
            serviceURL = new JMXServiceURL(url);
            jmxConnector = JMXConnectorFactory.connect(serviceURL);
            MBeanServerConnection mbeanConn = jmxConnector
                    .getMBeanServerConnection();

            List<ObjectNameRepresentation> result = new ArrayList();

            Set<ObjectName> beanSet = mbeanConn.queryNames(null, null);
            for (ObjectName name : beanSet) {
                ObjectNameRepresentation tmp = new ObjectNameRepresentation();
                tmp.setHost(host);
                tmp.setPort(port);
                tmp.setName(name.toString());

                List<String> attributes = new ArrayList();
                for (MBeanAttributeInfo attr : mbeanConn.getMBeanInfo(name)
                        .getAttributes()) {
                    attributes.add(attr.getName());
                }
                tmp.setAttributes(attributes);
                result.add(tmp);
            }

            return result;
        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (InstanceNotFoundException e) {
            logger.error(e.getMessage());
        } catch (IntrospectionException e) {
            logger.error(e.getMessage());
        } catch (ReflectionException e) {
            logger.error(e.getMessage());
        } finally {
            if (jmxConnector != null) {
                try {
                    jmxConnector.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }

        return new ArrayList<>();
    }
}