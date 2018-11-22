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

package org.ethereum.db;

import co.usc.core.BlockDifficulty;
import co.usc.crypto.Keccak256;
import co.usc.remasc.Sibling;
import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;
import org.ethereum.crypto.HashUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Mandeleil
 * @since 10.02.2015
 */
public class BlockStoreDummy implements BlockStore {

    @Override
    public byte[] getBlockHashByNumber(long blockNumber) {

        byte[] data = String.valueOf(blockNumber).getBytes(StandardCharsets.UTF_8);
        return HashUtil.keccak256(data);
    }

    @Override
    public byte[] getBlockHashByNumber(long blockNumber, byte[] branchBlockHash) {
        return getBlockHashByNumber(blockNumber);
    }

    @Override
    public Block getChainBlockByNumber(long blockNumber) {
        return null;
    }

    @Override
    public Block getBlockByHash(byte[] hash) {
        return null;
    }

    @Override
    public Block getBlockByHashAndDepth(byte[] hash, long depth) {
        return null;
    }

    @Override
    public boolean isBlockExist(byte[] hash) {
        return false;
    }

    @Override
    public List<byte[]> getListHashesEndWith(byte[] hash, long qty) {
        return null;
    }

    @Override
    public List<BlockHeader> getListHeadersEndWith(byte[] hash, long qty) {
        return null;
    }

    @Override
    public List<Block> getListBlocksEndWith(byte[] hash, long qty) {
        return null;
    }

    @Override
    public void saveBlock(Block block, BlockDifficulty cummDifficulty, boolean mainChain) {

    }

    @Override
    public void removeBlock(Block block) {
        // unused
    }

    @Override
    public Block getBestBlock() {
        return null;
    }

    @Override
    public void flush() {
    }

    @Override
    public long getMaxNumber() {
        return 0;
    }


    @Override
    public void reBranch(Block forkBlock) {

    }

    @Override
    public BlockDifficulty getTotalDifficultyForHash(byte[] hash) {
        return null;
    }

    @Override
    public List<Block> getChainBlocksByNumber(long blockNumber) {
        return new ArrayList<>();
    }

    @Override
    public List<BlockInformation> getBlocksInformationByNumber(long blockNumber) { return null; }

    @Override
    public Map<Long, List<Sibling>> getSiblingsFromBlockByHash(Keccak256 hash) {
        return null;
    }
}
