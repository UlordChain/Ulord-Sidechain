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

package co.usc.net.eth;

import co.usc.blockchain.utils.BlockGenerator;
import co.usc.config.TestSystemProperties;
import co.usc.net.Status;
import co.usc.net.messages.*;
import org.ethereum.core.Block;
import org.ethereum.net.eth.message.Eth62MessageFactory;
import org.ethereum.net.eth.message.EthMessage;
import org.ethereum.net.eth.message.EthMessageCodes;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by ajlopez on 5/14/2016.
 */
public class UscMessageTest {

    @Test
    public void encodeDecodeGetBlockMessage() {
        Block block = new BlockGenerator().getBlock(1);
        GetBlockMessage message = new GetBlockMessage(block.getHash().getBytes());
        UscMessage uscmessage = new UscMessage(message);

        byte[] encoded = uscmessage.getEncoded();

        Eth62MessageFactory factory = new Eth62MessageFactory();

        EthMessage ethmessage = (EthMessage)factory.create((byte)0x08, encoded);

        Assert.assertNotNull(ethmessage);
        Assert.assertEquals(EthMessageCodes.USC_MESSAGE, ethmessage.getCommand());

        UscMessage result = (UscMessage)ethmessage;

        Assert.assertNotNull(result.getMessage());

        Message resultMessage = result.getMessage();

        Assert.assertEquals(MessageType.GET_BLOCK_MESSAGE, resultMessage.getMessageType());
        Assert.assertArrayEquals(block.getHash().getBytes(), ((GetBlockMessage)resultMessage).getBlockHash());
    }

    @Test
    public void encodeDecodeStatusMessage() {
        Block block = new BlockGenerator().getBlock(1);
        Status status = new Status(block.getNumber(), block.getHash().getBytes());
        StatusMessage message = new StatusMessage(status);
        UscMessage uscmessage = new UscMessage(message);

        byte[] encoded = uscmessage.getEncoded();

        Eth62MessageFactory factory = new Eth62MessageFactory();

        EthMessage ethmessage = (EthMessage)factory.create((byte)0x08, encoded);

        Assert.assertNotNull(ethmessage);
        Assert.assertEquals(EthMessageCodes.USC_MESSAGE, ethmessage.getCommand());

        UscMessage result = (UscMessage)ethmessage;

        Assert.assertNotNull(result.getMessage());

        Message resultMessage = result.getMessage();

        Assert.assertEquals(MessageType.STATUS_MESSAGE, resultMessage.getMessageType());
        Assert.assertArrayEquals(block.getHash().getBytes(), ((StatusMessage)resultMessage).getStatus().getBestBlockHash());
        Assert.assertEquals(block.getNumber(), ((StatusMessage)resultMessage).getStatus().getBestBlockNumber());
    }

    @Test
    public void encodeDecodeBlockMessage() {
        Block block = new BlockGenerator().getBlock(1);
        BlockMessage message = new BlockMessage(block);
        UscMessage uscmessage = new UscMessage(message);

        byte[] encoded = uscmessage.getEncoded();

        Eth62MessageFactory factory = new Eth62MessageFactory();

        EthMessage ethmessage = (EthMessage)factory.create((byte)0x08, encoded);

        Assert.assertNotNull(ethmessage);
        Assert.assertEquals(EthMessageCodes.USC_MESSAGE, ethmessage.getCommand());

        UscMessage result = (UscMessage)ethmessage;

        Assert.assertNotNull(result.getMessage());

        Message resultMessage = result.getMessage();

        Assert.assertEquals(MessageType.BLOCK_MESSAGE, resultMessage.getMessageType());
        Assert.assertEquals(block.getHash(), ((BlockMessage)resultMessage).getBlock().getHash());
        Assert.assertArrayEquals(block.getEncoded(), ((BlockMessage)resultMessage).getBlock().getEncoded());
        Assert.assertEquals(block.getNumber(), ((BlockMessage)resultMessage).getBlock().getNumber());
    }
}
