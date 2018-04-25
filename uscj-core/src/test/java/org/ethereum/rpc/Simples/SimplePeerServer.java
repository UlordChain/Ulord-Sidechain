/*
 * This file is part of RskJ
 * Copyright (C) 2017 USC Labs Ltd.
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

package org.ethereum.rpc.Simples;

import org.ethereum.net.server.PeerServer;

import java.net.InetAddress;

/**
 * Created by Ruben on 15/06/2016.
 */
public class SimplePeerServer implements PeerServer {

    public boolean isListening = false;

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    public boolean isListening() {
        return isListening;
    }
}