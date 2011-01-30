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
package com.jetoile.jmanager4all.agent;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetoile.jmanager4all.JManager4All;

/**
 * @author khanh
 * 
 */
public class AgentLauncher {
	// public static class CleanThread extends Thread {
	// private final AgentLauncherThread cs;
	//
	// public CleanThread(AgentLauncherThread cs) {
	// super("JMX Agent Cleaner");
	// this.cs = cs;
	// setDaemon(true);
	// }
	//
	// @Override
	// public void run() {
	// boolean loop = true;
	// try {
	// while (loop) {
	// final Thread[] all = new Thread[Thread.activeCount() + 100];
	// final int count = Thread.enumerate(all);
	// loop = false;
	// for (int i = 0; i < count; i++) {
	// final Thread t = all[i];
	// // daemon: skip it.
	// if (t.isDaemon())
	// continue;
	// // RMI Reaper: skip it.
	// if (t.getName().startsWith("RMI Reaper"))
	// continue;
	// if (t.getName().startsWith("DestroyJavaVM"))
	// continue;
	// // Non daemon, non RMI Reaper: join it, break the for
	// // loop, continue in the while loop (loop=true)
	// loop = true;
	// try {
	// System.out.println("Waiting on " + t.getName() + " [id=" + t.getId() + "]");
	// t.join();
	// } catch (Exception ex) {
	// ex.printStackTrace();
	// }
	// break;
	// }
	// }
	// // We went through a whole for-loop without finding any
	// // thread to join. We can close cs.
	// } catch (Exception ex) {
	// ex.printStackTrace();
	// } finally {
	// try {
	// // if we reach here it means the only non-daemon threads
	// // that remain are reaper threads - or that we got an
	// // unexpected exception/error.
	// cs.stop();
	// } catch (Exception ex) {
	// ex.printStackTrace();
	// }
	// }
	// }
	// }

	public static void premain(String args) {
		String startDelayProperty = System.getProperty("startDelay");
		String portProperty = System.getProperty("port");
		String isServerProperty = System.getProperty("isServer");
		String ipv4Property = System.getProperty("ipv4");

		if (startDelayProperty == null || portProperty == null) {
			System.out.println("error : usage : -Dport=<int> -DstartDelay=<int> [-DisServer=<boolean>] [-Dipv4=<boolean>]");
			System.out.println("	- par défaut : isServer=true");
			System.out.println("	- par défaut : ipv4=true");
			System.out.println("java -javaagent:jmanager-1.0-SNAPSHOT.jar \\\"");
			System.out.println("			-classpath \"$CLASSPATH:logback.xml:jmanager-1.0-SNAPSHOT.jar\" \\ ");
			System.out.println("			-DstartDelay=2000 \\");
			System.out.println("			-Dport=18080 \\");
			System.out.println("			com.blogspot.jetoile.jmanager.sample.agent.TestJMXServerTest");
			System.exit(-1);
		}
		if (isServerProperty == null) {
			isServerProperty = "true";
		}
		if (ipv4Property == null) {
			ipv4Property = "true";
		}

		int startDelay = Integer.parseInt(startDelayProperty);
		int port = Integer.parseInt(portProperty);
		boolean isServer = Boolean.parseBoolean(isServerProperty);
		boolean ipv4 = Boolean.parseBoolean(ipv4Property);

		if (ipv4) {
			System.setProperty("java.net.preferIPv4Stack", "true");
		}

		AgentLauncherThread thread = new AgentLauncherThread(startDelay, port, isServer);
		thread.start();

		// Start the CleanThread.
		// final Thread clean = new CleanThread(thread);
		// clean.start();
	}
}

class AgentLauncherThread extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(AgentLauncherThread.class);

	private int startDelay = 2000;
	private int port = 18080;
	private boolean isServer = true;
	private JManager4All jmxServer = null;

	public AgentLauncherThread(final int startDelay, final int port, final boolean isServer) {
		this.startDelay = startDelay;
		this.port = port;
		this.isServer = isServer;
	}

	@Override
	public void destroy() {
		try {
			this.jmxServer.stop();
		} catch (IOException e) {
			LOGGER.error("error during stop... {}", e);
		}
	}

	@Override
	public void run() {
		try {
			Thread.sleep(this.startDelay);
		} catch (InterruptedException e) {
			LOGGER.error("error during start initializing... {}", e);
		}
		jmxServer = new JManager4All(this.port, this.isServer);
		jmxServer.start();

	}

}