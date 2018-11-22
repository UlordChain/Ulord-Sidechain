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

package co.usc.net.messages;

import org.ethereum.core.Block;
import org.ethereum.util.RLP;

/**
 * Created by ajlopez on 5/10/2016.
 */
public class BlockResponseMessage extends MessageWithId {
    private long id;
    private Block block;

    public BlockResponseMessage(long id, Block block) {
        this.id = id;
        this.block = block;
    }

    public long getId() {
        return this.id;
    }

    public Block getBlock() {
        return this.block;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.BLOCK_RESPONSE_MESSAGE;
    }

    @Override
    public byte[] getEncodedMessageWithoutId() {
        byte[] rlpBlock = RLP.encode(this.block.getEncoded());

        return RLP.encodeList(rlpBlock);
    }
}
