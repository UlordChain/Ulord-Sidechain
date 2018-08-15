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

import org.ethereum.net.message.Message;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ajlopez on 26/04/2017.
 */
public class MessageFilterTest {
    @Test
    public void filterWithoutFilteredCommandsAcceptsAnyMessage() {
        MessageFilter filter = new MessageFilter(null);

        Message uscMessage = WriterMessageRecorderTest.createUscMessage();
        Message ethMessage = WriterMessageRecorderTest.createEthMessage();

        Assert.assertTrue(filter.acceptMessage(uscMessage));
        Assert.assertTrue(filter.acceptMessage(ethMessage));
    }

    @Test
    public void filterWithEmptyCommandsAcceptsAnyMessage() {
        List<String> commands = new ArrayList<>();

        MessageFilter filter = new MessageFilter(commands);

        Message uscMessage = WriterMessageRecorderTest.createUscMessage();
        Message ethMessage = WriterMessageRecorderTest.createEthMessage();

        Assert.assertTrue(filter.acceptMessage(uscMessage));
        Assert.assertTrue(filter.acceptMessage(ethMessage));
    }

    @Test
    public void filterAcceptsEthMessageWhenCommandIsSpecified() {
        List<String> commands = new ArrayList<>();
        commands.add("TRANSACTIONS");

        MessageFilter filter = new MessageFilter(commands);

        Message uscMessage = WriterMessageRecorderTest.createUscMessage();
        Message ethMessage = WriterMessageRecorderTest.createEthMessage();

        Assert.assertFalse(filter.acceptMessage(uscMessage));
        Assert.assertTrue(filter.acceptMessage(ethMessage));
    }

    @Test
    public void filterAcceptUscMessageWhenCommandIsSpecified() {
        List<String> commands = new ArrayList<>();
        commands.add("USC_MESSAGE");

        MessageFilter filter = new MessageFilter(commands);

        Message uscMessage = WriterMessageRecorderTest.createUscMessage();
        Message ethMessage = WriterMessageRecorderTest.createEthMessage();

        Assert.assertTrue(filter.acceptMessage(uscMessage));
        Assert.assertFalse(filter.acceptMessage(ethMessage));
    }

    @Test
    public void filterMessagesWhenCommandsAreSpecified() {
        List<String> commands = new ArrayList<>();
        commands.add("USC_MESSAGE");
        commands.add("TRANSACTIONS");
        commands.add("BLOCKS");

        MessageFilter filter = new MessageFilter(commands);

        Message uscMessage = WriterMessageRecorderTest.createUscMessage();
        Message ethMessage = WriterMessageRecorderTest.createEthMessage();

        Assert.assertTrue(filter.acceptMessage(uscMessage));
        Assert.assertTrue(filter.acceptMessage(ethMessage));
    }

    @Test
    public void filterMessagesWhenUscMessageTypeIsSpecified() {
        List<String> commands = new ArrayList<>();
        commands.add("USC_MESSAGE:GET_BLOCK_MESSAGE");
        commands.add("TRANSACTIONS");
        commands.add("BLOCKS");

        MessageFilter filter = new MessageFilter(commands);

        Message uscMessage = WriterMessageRecorderTest.createUscMessage();
        Message ethMessage = WriterMessageRecorderTest.createEthMessage();

        Assert.assertTrue(filter.acceptMessage(uscMessage));
        Assert.assertTrue(filter.acceptMessage(ethMessage));
    }

    @Test
    public void rejectUscMessageIfMessageTypeDoesNotMatch() {
        List<String> commands = new ArrayList<>();
        commands.add("USC_MESSAGE:GET_BLOCKS_MESSAGE");
        commands.add("TRANSACTIONS");
        commands.add("BLOCKS");

        MessageFilter filter = new MessageFilter(commands);

        Message uscMessage = WriterMessageRecorderTest.createUscMessage();
        Message ethMessage = WriterMessageRecorderTest.createEthMessage();

        Assert.assertFalse(filter.acceptMessage(uscMessage));
        Assert.assertTrue(filter.acceptMessage(ethMessage));
    }
}
