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

package org.ethereum.rpc;

import co.usc.blockchain.utils.BlockGenerator;
import org.ethereum.core.Block;
import org.ethereum.jsontestsuite.builder.BlockBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.bouncycastle.util.encoders.Hex;

/**
 * Created by ajlopez on 17/01/2018.
 */
public class NewBlockFilterTest {
    @Test
    public void noEvents() {
        NewBlockFilter filter = new NewBlockFilter();

        Object[] result = filter.getEvents();

        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void oneBlockAndEvent() {
        NewBlockFilter filter = new NewBlockFilter();
        Block block = new BlockGenerator().getBlock(1);

        filter.newBlockReceived(block);

        Object[] result = filter.getEvents();

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("0x" + block.getHash(), result[0]);
    }

    @Test
    public void twoBlocksAndEvents() {
        NewBlockFilter filter = new NewBlockFilter();
        Block block1 = new BlockGenerator().getBlock(1);
        Block block2 = new BlockGenerator().getBlock(2);

        filter.newBlockReceived(block1);
        filter.newBlockReceived(block2);

        Object[] result = filter.getEvents();

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.length);
        Assert.assertEquals("0x" + block1.getHash(), result[0]);
        Assert.assertEquals("0x" + block2.getHash(), result[1]);
    }
}
