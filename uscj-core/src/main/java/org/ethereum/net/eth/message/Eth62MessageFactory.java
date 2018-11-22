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

package org.ethereum.net.eth.message;

import co.usc.net.eth.UscMessage;
import org.ethereum.net.message.Message;
import org.ethereum.net.message.MessageFactory;

import static org.ethereum.net.eth.EthVersion.V62;

/**
 * @author Mikhail Kalinin
 * @since 04.09.2015
 */
public class Eth62MessageFactory implements MessageFactory {

    @Override
    public Message create(byte code, byte[] encoded) {

        EthMessageCodes receivedCommand = EthMessageCodes.fromByte(code, V62);
        switch (receivedCommand) {
            case STATUS:
                return new StatusMessage(encoded);
            case NEW_BLOCK_HASHES:
                return new NewBlockHashesMessage(encoded);
            case TRANSACTIONS:
                return new TransactionsMessage(encoded);
            case GET_BLOCK_HEADERS:
                return new GetBlockHeadersMessage(encoded);
            case BLOCK_HEADERS:
                return new BlockHeadersMessage(encoded);
            case GET_BLOCK_BODIES:
                return new GetBlockBodiesMessage(encoded);
            case BLOCK_BODIES:
                return new BlockBodiesMessage(encoded);
            case NEW_BLOCK:
                return new NewBlockMessage(encoded);
            // USC new message
            case USC_MESSAGE:
                return new UscMessage(encoded);
            default:
                throw new IllegalArgumentException("No such message");
        }
    }
}
