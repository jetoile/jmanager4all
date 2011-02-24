/*
 * Copyright (c) 2011 Khanh Tuong Maudoux <kmx.petals@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jetoile.jmanager4all.jmx;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetoile.jmanager4all.jmx.handler.RemoteMBeanHandler;
import com.jetoile.jmanager4all.jmx.handler.RemoteMXBeanHandler;
import com.jetoile.jmanager4all.pojo.JManagerAddress;
import com.sun.management.HotSpotDiagnosticMXBean;

/**
 * Register and unregister remote mBean in the current MBeanServer
 * 
 * @author khanh
 * 
 */
public class MBeanHandler extends Thread {
    private static final String DOMAIN_REMOTE = "remote";

    private static final Logger LOGGER = LoggerFactory.getLogger(MBeanHandler.class);

    private final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    private final JMXConnectorStubCache.Event event;
    private final JMXConnector connector;
    private final JManagerAddress address;

    private final RemoteMBeanHandler remoteMBeanHandler = new RemoteMBeanHandler();
    private final RemoteMXBeanHandler remoteMXBeanHandler = new RemoteMXBeanHandler();

    public MBeanHandler(final JMXConnectorStubCache.Event event, final JManagerAddress address, final JMXConnector connector) {
        this.event = event;
        this.connector = connector;
        this.address = address;
    }

    @Override
    public void run() {
        try {
            switch (event) {
            case ADD:
                LOGGER.debug("add {}", connector);
                handleAdd();
                break;
            case REMOVE:
                LOGGER.debug("remove {}", connector);
                handleRemove();
                break;
            }
        } catch (Exception e) {
            // trop d'exceptions tuent l'exception... ;-)
            LOGGER.error("error during update process update: {}", e);
        }

    }

    void handleRemove() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        final ObjectName queryObjectName = new ObjectName(DOMAIN_REMOTE + ":instance=" + address + ",*");
        final Set<ObjectName> objectNames = mbeanServer.queryNames(queryObjectName, null);
        for (ObjectName objectName : objectNames) {
            LOGGER.debug("remove from mBeanServer objectName : {}", objectName);
            mbeanServer.unregisterMBean(objectName);
        }
    }

    void handleAdd() throws IOException, MalformedObjectNameException, InstanceNotFoundException, IntrospectionException, ReflectionException,
            ClassNotFoundException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        if (connector != null) {
            connector.connect();
            ObjectName objectName = new ObjectName("*:*");
            final MBeanServerConnection mBeanServerConnection = connector.getMBeanServerConnection();

            final Set<ObjectInstance> instances = mBeanServerConnection.queryMBeans(objectName, null);
            // final Set<ObjectInstance> instancesSet = mBeanServerConnection.queryMBeans(objectName, null);
            // final ObjectInstance[] instances = instancesSet.toArray(new ObjectInstance[0]);

            LOGGER.debug("{}", mBeanServerConnection.getMBeanCount());

            for (ObjectInstance objectInstance : instances) {
                final ObjectName distantObjectName = objectInstance.getObjectName();
                if (DOMAIN_REMOTE.equals(distantObjectName.getDomain())) {
                    continue;
                }
                final ObjectName newObjectName = new ObjectName(DOMAIN_REMOTE + ":instance=" + address + ", " + "subdomain=" + distantObjectName.getDomain()
                        + ", " + distantObjectName.getKeyPropertyListString());
                // final ObjectName newObjectName = new ObjectName("remote:instance=" + connectionId.substring(connectionId.indexOf("://") + 3) + ", "
                // + "subdomain=" + distantObjectName.getDomain() + ", " + distantObjectName.getKeyPropertyListString());
                final MBeanInfo mBeanInfo = mBeanServerConnection.getMBeanInfo(distantObjectName);
                registerRemoteMBean(mBeanServerConnection, distantObjectName, newObjectName, mBeanInfo);
            }
            handleSpecificMXBean(mBeanServerConnection);
        }
    }

    private void registerRemoteMBean(final MBeanServerConnection mBeanServerConnection, final ObjectName distantObjectName, final ObjectName newObjectName,
            final MBeanInfo mBeanInfo) throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException, IOException, ClassNotFoundException {
        Object mBean = this.remoteMBeanHandler.getRemoteMBean(mBeanServerConnection, mBeanInfo, distantObjectName);
        if (mBean == null) {
            mBean = this.remoteMXBeanHandler.getRemoteMBean(mBeanServerConnection, mBeanInfo, distantObjectName);
        }
        if (mBean != null) {
            mbeanServer.registerMBean(mBean, newObjectName);
        }
    }

    private void handleSpecificMXBean(final MBeanServerConnection mBeanServerConnection) throws MalformedObjectNameException, InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException, IOException {
        // cf. http://download.oracle.com/javase/6/docs/api/

        // traitement particulier pour les MXBeans de type MemoryPoolMXBean
        registerOtherMxBean(mBeanServerConnection, ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE, MemoryPoolMXBean.class);
        // traitement particulier pour les MXBeans de type MemoryManagerMXBean
        registerOtherMxBean(mBeanServerConnection, ManagementFactory.MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE, MemoryManagerMXBean.class);
        // traitement particulier pour les MXBeans de type GarbageCollector
        registerOtherMxBean(mBeanServerConnection, ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE, GarbageCollectorMXBean.class);

        // traitement particulier pour les MXBeans de type Hotspot
        registerOtherMxBean(mBeanServerConnection, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
    }

    private void registerOtherMxBean(final MBeanServerConnection mBeanServerConnection, String type, Class<?> clazz) throws MalformedObjectNameException,
            IOException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        final ObjectName requestedObjectName = new ObjectName(type + ",*");
        final Set<ObjectName> objectNames = mBeanServerConnection.queryNames(requestedObjectName, null);

        for (ObjectName objectName : objectNames) {
            final Object proxy = ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, objectName.getCanonicalName(), clazz);
            final ObjectName newObjectName = new ObjectName("remote:instance=" + address + ", " + "subdomain=" + objectName.getDomain() + ", "
                    + objectName.getKeyPropertyListString());
            mbeanServer.registerMBean(proxy, newObjectName);
        }
    }
}
