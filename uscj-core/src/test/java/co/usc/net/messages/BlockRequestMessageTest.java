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

package co.usc.net.messages;

import co.usc.blockchain.utils.BlockGenerator;
import co.usc.blockchain.utils.BlockGenerator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by ajlopez on 5/11/2016.
 */
public class BlockRequestMessageTest {
    @Test
    public void createWithBlockHash() {
        byte[] hash = new BlockGenerator().getGenesisBlock().getHash().getBytes();
        BlockRequestMessage message = new BlockRequestMessage(100, hash);

        Assert.assertEquals(100, message.getId());
        Assert.assertArrayEquals(hash, message.getBlockHash());
        Assert.assertEquals(MessageType.BLOCK_REQUEST_MESSAGE, message.getMessageType());
    }
}