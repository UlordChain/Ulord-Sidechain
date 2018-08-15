/*
 * This file is part of USC
 * Copyright (C) 2016 - 2018 USC developer team.
 * (derived from ethereumJ library, Copyright (c) 2016 <ether.camp>)
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

package org.ethereum.net.eth.handler;

import co.usc.net.eth.UscWireProtocol;
import org.ethereum.net.eth.EthVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Default factory implementation
 *
 * @author Mikhail Kalinin
 * @since 20.08.2015
 */
@Component
public class EthHandlerFactoryImpl implements EthHandlerFactory {

    private final UscWireProtocolFactory uscWireProtocolFactory;

    @Autowired
    public EthHandlerFactoryImpl(UscWireProtocolFactory uscWireProtocolFactory) {
        this.uscWireProtocolFactory = uscWireProtocolFactory;
    }

    @Override
    public EthHandler create(EthVersion version) {
        switch (version) {
            case V62:
                return uscWireProtocolFactory.newInstance();

            default:    throw new IllegalArgumentException("Eth " + version + " is not supported");
        }
    }

    public interface UscWireProtocolFactory {
        UscWireProtocol newInstance();
    }
}
