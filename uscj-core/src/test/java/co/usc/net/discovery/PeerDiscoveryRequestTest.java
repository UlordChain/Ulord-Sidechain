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

package co.usc.net.discovery;

import co.usc.net.discovery.message.DiscoveryMessageType;
import co.usc.net.discovery.message.PingPeerMessage;
import co.usc.net.discovery.message.PongPeerMessage;
import co.usc.net.discovery.table.PeerDiscoveryRequestBuilder;
import org.ethereum.crypto.ECKey;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Created by mario on 20/02/17.
 */
public class PeerDiscoveryRequestTest {

    public static final int NETWORK_ID = 1;

    @Test
    public void create() {
        ECKey key = new ECKey();
        String check = UUID.randomUUID().toString();
        PingPeerMessage pingPeerMessage = PingPeerMessage.create("localhost", 80, check, key, NETWORK_ID);
        PongPeerMessage pongPeerMessage = PongPeerMessage.create("localhost", 80, check, key, NETWORK_ID);
        InetSocketAddress address = new InetSocketAddress("localhost", 8080);

        PeerDiscoveryRequest request = PeerDiscoveryRequestBuilder.builder().messageId(check)
                .message(pingPeerMessage).address(address).expectedResponse(DiscoveryMessageType.PONG)
                .expirationPeriod(1000).attemptNumber(1).build();

        Assert.assertNotNull(request);
        Assert.assertTrue(request.validateMessageResponse(pongPeerMessage));
        Assert.assertFalse(request.validateMessageResponse(pingPeerMessage));
    }
}
