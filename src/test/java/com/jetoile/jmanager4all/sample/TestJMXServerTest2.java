package com.jetoile.jmanager4all.sample;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.jgroups.ChannelException;

import com.jetoile.jmanager4all.JManager4All;
import com.jetoile.jmanager4all.sample.service.Test2;

public class TestJMXServerTest2 {

	public static void main(String[] args) throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException,
			NotCompliantMBeanException, NullPointerException, MalformedURLException, IOException, ChannelException, InterruptedException {

		JManager4All jmxServer = new JManager4All(18081);
		jmxServer.start();
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		mBeanServer.registerMBean(new Test2(), new ObjectName("com.blogspot.jetoile:name=toto2"));
	}
}
