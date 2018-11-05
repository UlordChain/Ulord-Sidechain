/*
 * This file is part of USC
 * Copyright (C) 2016 - 2018 USC developer team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.usc.net.simples;

import co.usc.net.MessageChannel;
import co.usc.net.NodeID;
import co.usc.net.messages.Message;

import java.net.InetAddress;

/**
 * Created by ajlopez on 5/14/2016.
 */
public class SimpleNodeChannel implements MessageChannel {
    private SimpleNode sender;
    private SimpleNode receiver;
    private NodeID nodeID = new NodeID(new byte[]{});

    public SimpleNodeChannel(SimpleNode sender, SimpleNode receiver) {
        this.sender = sender;
        this.receiver = receiver;

        if (receiver != null)
            this.nodeID = receiver.getNodeID();
    }

    public void sendMessage(Message message) {
        if (this.receiver != null)
            this.receiver.receiveMessageFrom(this.sender, message);
    }

    public NodeID getPeerNodeID() {
        return this.nodeID;
    }

    @Override
    public void setPeerNodeID(NodeID peerNodeId) {

    }

    @Override
    public void setAddress(InetAddress address) { }

    @Override
    public InetAddress getAddress() { return null; }
}
