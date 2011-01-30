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
package com.jetoile.jmanager4all.jgroups.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.MembershipListener;
import org.jgroups.View;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetoile.jmanager4all.jmx.JMXConnectorStubCache;
import com.jetoile.jmanager4all.pojo.JManagerAddress;
import com.jetoile.jmanager4all.pojo.JManagerConnector;

/**
 * @author khanh
 * 
 */
public class JGroupsChangeSetListener implements MembershipListener {

	final static private Logger LOGGER = LoggerFactory.getLogger(JGroupsChangeSetListener.class);

	final private JMXConnectorStubCache connectorsStub = new JMXConnectorStubCache();

	final private Channel privateChannel;

	private RpcDispatcher rpcDispatcher;

	public JGroupsChangeSetListener(final Channel privateChannel) {
		this.privateChannel = privateChannel;
	}

	public void setRpcDispatcher(RpcDispatcher rpcDispatcher) {
		this.rpcDispatcher = rpcDispatcher;
	}

	@Override
	synchronized public void viewAccepted(View new_view) {
		// when a new member is up
		List<Address> newAddresses = getNewAddresses(new_view.getMembers());

		newAddresses.remove(privateChannel.getAddress());

		List<Address> ads = new ArrayList<Address>();
		for (Address ad : newAddresses) {
			if (!connectorsStub.containsKey(new JManagerAddress(ad.toString()))) {
				ads.add(ad);
			}
		}

		if (!ads.isEmpty()) {

			MethodCall methodCall = new MethodCall("getStubConnector", new Object[] {}, new Class[] {});
			LOGGER.debug("invoke remote getStubConnector on: {}", ads);

			// RequestOptions requestOption = new RequestOptions();
			RspList resps = rpcDispatcher.callRemoteMethods(ads, methodCall, RequestOptions.SYNC);
			LOGGER.debug("after invoke getStubConnector - nb result {}", resps.numReceived());

			if (resps.numReceived() == 0) {
				LOGGER.debug("retry...");
				resps = rpcDispatcher.callRemoteMethods(ads, methodCall, RequestOptions.SYNC);
			}

			for (Object resp : resps.getResults()) {
				JManagerConnector connector = (JManagerConnector) resp;
				LOGGER.debug("new jmxConnector: {}", connector);
				JManagerAddress privateAddress = new JManagerAddress(connector.getLocation().toString());
				connectorsStub.put(privateAddress, connector.getConnector());
			}
		}

		List<JManagerAddress> members = new ArrayList<JManagerAddress>();
		for (Address member : new_view.getMembers()) {
			members.add(new JManagerAddress(member.toString()));
		}
		List<JManagerAddress> olds = getObsoleteAddresses(members);
		for (JManagerAddress old : olds) {
			LOGGER.debug("remove jmxConnector: {}", old);
			connectorsStub.remove(new JManagerAddress(old.toString()));
		}
	}

	@Override
	public void suspect(Address suspected_mbr) {
		// NOTHING TO DO
	}

	@Override
	public void block() {
		// NOTHING TO DO
	}

	List<Address> getNewAddresses(Vector<Address> newMembers) {
		List<Address> result = new ArrayList<Address>();
		for (Address address : newMembers) {
			if (!this.connectorsStub.containsKey(new JManagerAddress(address.toString()))) {
				result.add(address);
			}
		}
		return result;
	}

	List<JManagerAddress> getObsoleteAddresses(List<JManagerAddress> newMembers) {
		List<JManagerAddress> result = new ArrayList<JManagerAddress>();
		for (JManagerAddress address : this.connectorsStub.keySet()) {
			if (!newMembers.contains(address)) {
				result.add(address);
			}
		}
		return result;
	}

}
