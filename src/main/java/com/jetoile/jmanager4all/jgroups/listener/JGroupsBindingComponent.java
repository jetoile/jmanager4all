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

import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.ReceiverAdapter;
import org.jgroups.blocks.RpcDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jetoile.jmanager4all.JManagerBindingComponent;
import com.jetoile.jmanager4all.jmx.JMXConnectorStubCache;
import com.jetoile.jmanager4all.pojo.JManagerAddress;
import com.jetoile.jmanager4all.pojo.JManagerConnector;

/**
 * @author khanh
 * 
 */
public class JGroupsBindingComponent extends ReceiverAdapter implements JManagerBindingComponent {

    static final private Logger LOGGER = LoggerFactory.getLogger(JGroupsBindingComponent.class);

    private RpcDispatcher rpcDispatcher;
    private Channel channel;

    private boolean isServer = true;

    final private JManagerConnector jmanagerConnector;

    private JMXConnectorStubCache connectorsStub = null;

    public JGroupsBindingComponent(final JManagerConnector jmanagerConnector, final boolean isServer) {
        this.isServer = isServer;
        this.jmanagerConnector = jmanagerConnector;
    }

    public void setConnectorsStub(JMXConnectorStubCache connectorsStub) {
        this.connectorsStub = connectorsStub;
    }

    @Override
    public void start() {
        if (this.connectorsStub != null) {
            try {
                // start a private channel to response for its jmxConnectorStub
                this.channel = new JChannel("default-udp.xml");

                if (isServer) {
                    final JGroupsChangeSetListener changeSetListener = new JGroupsChangeSetListener(channel, this.connectorsStub);
                    rpcDispatcher = new RpcDispatcher(this.channel, null, changeSetListener, this);
                    changeSetListener.setRpcDispatcher(rpcDispatcher);
                } else {
                    rpcDispatcher = new RpcDispatcher(this.channel, null, null, this);
                }
                this.channel.connect("privateJMXChannel");
                JManagerAddress privateAddress = new JManagerAddress(this.channel.getAddress().toString());
                this.jmanagerConnector.setLocation(privateAddress);
            } catch (ChannelException e) {
                LOGGER.error("cannot create channel: {}", e);
            }
        } else {
            LOGGER.warn("connectorsStub should be set");
        }

    }

    @Override
    public void stop() {
        this.channel.close();
    }

    @Override
    public JManagerConnector getStubConnector() {
        return this.jmanagerConnector;
    }

}
