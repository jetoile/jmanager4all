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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.remote.JMXConnector;

import com.jetoile.jmanager4all.pojo.JManagerAddress;

/**
 * @author khanh
 * 
 */
public class JMXConnectorStubCache implements Serializable {
	public static enum Event {
		ADD, REMOVE
	}

	volatile private Map<JManagerAddress, JMXConnector> connectorsStub = Collections.synchronizedMap(new HashMap<JManagerAddress, JMXConnector>());

	synchronized public Map<JManagerAddress, JMXConnector> getValue() {
		return this.connectorsStub;
	}

	synchronized public JMXConnector put(JManagerAddress key, JMXConnector value) {
		JMXConnector result = this.connectorsStub.put(key, value);
		Thread mBeanHandler = new MBeanHandler(Event.ADD, key, value);
		mBeanHandler.run();
		return result;
	}

	synchronized public Set<JManagerAddress> keySet() {
		return this.connectorsStub.keySet();
	}

	synchronized public JMXConnector remove(JManagerAddress key) {
		JMXConnector result = this.connectorsStub.remove(key);
		Thread mBeanHandler = new MBeanHandler(Event.REMOVE, key, result);
		mBeanHandler.run();
		return result;
	}

	synchronized public JMXConnector get(JManagerAddress key) {
		return this.connectorsStub.get(key);
	}

	synchronized public boolean containsKey(JManagerAddress key) {
		return this.connectorsStub.containsKey(key);
	}

	synchronized public boolean containsValue(JMXConnector value) {
		return this.connectorsStub.containsValue(value);
	}

	synchronized public boolean isEmpty() {
		return this.connectorsStub.isEmpty();
	}

	synchronized public int size() {
		return this.connectorsStub.size();
	}

}
