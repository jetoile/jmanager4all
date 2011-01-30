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
package com.jetoile.jmanager4all.jmx.handler;

import java.io.IOException;

import javax.management.InstanceAlreadyExistsException;
import javax.management.JMX;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetoile.jmanager4all.jmx.MBeanWrapper;

/**
 * @author khanh
 * 
 */
public class RemoteMBeanHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteMBeanHandler.class);

	public RemoteMBeanHandler() {
	}

	public Object getRemoteMBean(final MBeanServerConnection mBeanServerConnection, final MBeanInfo mBeanInfo, final ObjectName distantObjectName)
			throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, IOException,
			ClassNotFoundException {
		if (!Boolean.parseBoolean((String) mBeanInfo.getDescriptor().getFieldValue("mxbean"))) {
			LOGGER.debug("current mxBeanInfo : {}", mBeanInfo);
			final Class<?> clazz = Class.forName(mBeanInfo.getClassName());
			final Class<?>[] interfazes = clazz.getInterfaces();
			if (interfazes.length == 1) {
				LOGGER.debug("current interface : {}", interfazes[0]);
				final Object proxy = JMX.newMBeanProxy(mBeanServerConnection, distantObjectName, interfazes[0]);
				final MBeanWrapper mBeanWrapper = new MBeanWrapper(mBeanInfo, proxy);
				return mBeanWrapper;
			}
		}
		return null;
	}
}
