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

package co.usc.remasc;

import co.usc.blockchain.utils.BlockGenerator;
import org.ethereum.core.Block;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by ajlopez on 13/04/2017.
 */
public class SiblingTest {
    @Test
    public void siblingSerializeWithGenesis() {
        Block genesis = new BlockGenerator().getGenesisBlock();

        Sibling sibling = new Sibling(genesis.getHeader(), genesis.getCoinbase(), 1);

        byte[] bytes = sibling.getEncoded();

        Assert.assertNotNull(bytes);

        Sibling result = Sibling.create(bytes);

        Assert.assertNotNull(result);

        Assert.assertArrayEquals(sibling.getHash(), result.getHash());
        Assert.assertEquals(sibling.getIncludedBlockCoinbase(), result.getIncludedBlockCoinbase());
        Assert.assertArrayEquals(sibling.getEncoded(), result.getEncoded());

        Assert.assertEquals(sibling.getIncludedHeight(), result.getIncludedHeight());
        Assert.assertEquals(sibling.getPaidFees(), result.getPaidFees());
    }

    @Test
    public void siblingSerializeWithBlock() {
        BlockGenerator blockGenerator = new BlockGenerator();
        Block genesis = blockGenerator.getGenesisBlock();
        Block block = blockGenerator.createChildBlock(genesis);

        Sibling sibling = new Sibling(block.getHeader(), block.getCoinbase(), 0);

        byte[] bytes = sibling.getEncoded();

        Assert.assertNotNull(bytes);

        Sibling result = Sibling.create(bytes);

        Assert.assertNotNull(result);

        Assert.assertArrayEquals(sibling.getHash(), result.getHash());
        Assert.assertEquals(sibling.getIncludedBlockCoinbase(), result.getIncludedBlockCoinbase());
        Assert.assertArrayEquals(sibling.getEncoded(), result.getEncoded());

        Assert.assertEquals(sibling.getIncludedHeight(), result.getIncludedHeight());
        Assert.assertEquals(sibling.getPaidFees(), result.getPaidFees());
    }
}
