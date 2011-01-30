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
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.LoggingMXBean;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author khanh
 * 
 */
public class RemoteMXBeanHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteMXBeanHandler.class);

	final private static Map<String, Class<?>> MXBEAN_MAP = new HashMap<String, Class<?>>();
	static {
		MXBEAN_MAP.put(ManagementFactory.COMPILATION_MXBEAN_NAME, CompilationMXBean.class);
		MXBEAN_MAP.put(ManagementFactory.CLASS_LOADING_MXBEAN_NAME, ClassLoadingMXBean.class);
		MXBEAN_MAP.put(ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
		MXBEAN_MAP.put(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
		MXBEAN_MAP.put(ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
		MXBEAN_MAP.put(ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
		MXBEAN_MAP.put("java.util.logging:type=Logging", LoggingMXBean.class);
	}

	public RemoteMXBeanHandler() {
	}

	public Object getRemoteMBean(final MBeanServerConnection mBeanServerConnection, final MBeanInfo mBeanInfo, final ObjectName distantObjectName)
			throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, IOException {
		if (Boolean.parseBoolean((String) mBeanInfo.getDescriptor().getFieldValue("mxbean"))) {
			LOGGER.debug("current mxBeanInfo : {}", mBeanInfo);
			Object proxy = getMxBeanProxy(mBeanServerConnection, mBeanInfo, distantObjectName);
			return proxy;
		}
		return null;
	}

	private Object getMxBeanProxy(final MBeanServerConnection mBeanServerConnection, final MBeanInfo mBeanInfo, final ObjectName distantObjectName)
			throws IOException {
		String distantName = distantObjectName.toString();
		int index = distantName.indexOf(",");
		String substring = distantName.substring(0, (index != -1) ? index : distantName.length());
		Class<?> mBeanClass = MXBEAN_MAP.get(substring);
		if (mBeanClass != null) {
			return ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, distantName, mBeanClass);
		} else if (!StringUtils.equals(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE, substring)
				&& !StringUtils.equals(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE, substring)
				&& !StringUtils.equals(ManagementFactory.MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE, substring)
				&& !StringUtils.contains(substring, "com.sun.management")) {
			return ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
		} else {
			return null;
		}
	}
}
