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
package com.jetoile.jmanager4all;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetoile.jmanager4all.jgroups.listener.JGroupsBindingComponent;
import com.jetoile.jmanager4all.pojo.JManagerConnector;

/**
 * @author khanh
 * 
 */
public class JManager4All {

	final static private Logger LOGGER = LoggerFactory.getLogger(JManager4All.class);

	// private static final int STATE_TIMEOUT = 500;
	private static final String CONNECTOR_PROTOCOL = "rmi";

	private boolean isServer = true;
	private JMXConnectorServer jmxConnector;
	private JMXServiceURL jmxServiceUrl;
	final private JManagerConnector jmanagerConnector = new JManagerConnector();
	private final MBeanServer mBeanServer;

	private JManagerBindingComponent jmanagerBindingComponent;

	public JManager4All(final int port) {
		try {
			this.jmxServiceUrl = new JMXServiceURL(CONNECTOR_PROTOCOL, null, port);
		} catch (MalformedURLException e) {
			LOGGER.error("unable to create JMXServiceURL: {}", e);
		}
		this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
		init();
	}

	public JManager4All(final int port, final boolean isServer) {
		this.isServer = isServer;
		try {
			this.jmxServiceUrl = new JMXServiceURL(CONNECTOR_PROTOCOL, null, port);
		} catch (MalformedURLException e) {
			LOGGER.error("unable to create JMXServiceURL: {}", e);
		}
		this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
		init();
	}

	private void init() {
		try {
			this.jmxConnector = JMXConnectorServerFactory.newJMXConnectorServer(this.jmxServiceUrl, null, mBeanServer);
			this.jmxConnector.start();

			final ObjectName objectName = new ObjectName(":type=csserver, name=csserver");
			mBeanServer.registerMBean(this.jmxConnector, objectName);
			this.jmanagerConnector.setConnector(this.jmxConnector.toJMXConnector(null));

			this.jmanagerBindingComponent = new JGroupsBindingComponent(this.jmanagerConnector, isServer);
		} catch (IOException e) {
			LOGGER.error("unable to init: {}", e);
		} catch (MalformedObjectNameException e) {
			LOGGER.error("unable to init: {}", e);
		} catch (NullPointerException e) {
			LOGGER.error("unable to init: {}", e);
		} catch (InstanceAlreadyExistsException e) {
			LOGGER.error("unable to init: {}", e);
		} catch (MBeanRegistrationException e) {
			LOGGER.error("unable to init: {}", e);
		} catch (NotCompliantMBeanException e) {
			LOGGER.error("unable to init: {}", e);
		}
	}

	public void stop() throws IOException {
		this.jmxConnector.stop();
		this.jmanagerBindingComponent.stop();
	}

	public void start() {
		this.jmanagerBindingComponent.start();
	}

	public JManagerConnector getStubConnector() {
		return this.jmanagerConnector;
	}

	// @Override
	// public byte[] getState() {
	// try {
	// return Util.objectToByteBuffer(this.connector);
	// } catch (Exception e) {
	// LOGGER.error("getState error", e);
	// }
	// return null;
	// }
	//
	// @Override
	// public void setState(byte[] state) {
	// try {
	// Connector connector = (Connector)Util.objectFromByteBuffer(state);
	// connectorsStub.put(connector.getPrivateAddress(),
	// connector.getConnector());
	// } catch (Exception e) {
	// LOGGER.error("setState error", e);
	// }
	// }
}
