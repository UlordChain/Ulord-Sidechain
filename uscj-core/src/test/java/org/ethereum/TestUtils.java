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

package org.ethereum;

import co.usc.core.Coin;
import co.usc.core.UscAddress;
import co.usc.crypto.Keccak256;
import org.apache.commons.lang3.StringUtils;
import org.ethereum.core.Block;
import org.ethereum.crypto.HashUtil;
import org.ethereum.db.IndexedBlockStore;
import org.ethereum.vm.DataWord;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.ethereum.crypto.HashUtil.EMPTY_TRIE_HASH;
import static org.ethereum.db.IndexedBlockStore.BLOCK_INFO_SERIALIZER;

public final class TestUtils {

    private TestUtils() {
    }

    public static byte[] randomBytes(int length) {
        byte[] result = new byte[length];
        new Random().nextBytes(result);
        return result;
    }

    public static DataWord randomDataWord() {
        return new DataWord(randomBytes(32));
    }

    public static UscAddress randomAddress() {
        return new UscAddress(randomBytes(20));
    }

    public static Keccak256 randomHash() {
        return new Keccak256(randomBytes(32));
    }

    public static Map<Long, List<IndexedBlockStore.BlockInfo>> createIndexMap(DB db){

        Map<Long, List<IndexedBlockStore.BlockInfo>> index = db.hashMapCreate("index")
                .keySerializer(Serializer.LONG)
                .valueSerializer(BLOCK_INFO_SERIALIZER)
                .makeOrGet();

        return index;
    }

    public static DB createMapDB(String testDBDir){

        String blocksIndexFile = testDBDir + "/blocks/index";
        File dbFile = new File(blocksIndexFile);
        if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();

        DB db = DBMaker.fileDB(dbFile)
                .transactionDisable()
                .closeOnJvmShutdown()
                .make();


        return db;
    }

    public static List<Block> getRandomChain(byte[] startParentHash, long startNumber, long length){

        List<Block> result = new ArrayList<>();

        byte[] lastHash = startParentHash;
        long lastIndex = startNumber;


        for (int i = 0; i < length; ++i){

            byte[] difficutly = new BigInteger(8, new Random()).toByteArray();
            byte[] newHash = HashUtil.randomHash();

            Block block = new Block(lastHash, newHash,  UscAddress.nullAddress().getBytes(), null, difficutly, lastIndex, new byte[] {0}, 0, 0, null, null,
                    null, null, EMPTY_TRIE_HASH, HashUtil.randomHash(), null, null, null, Coin.ZERO);

            ++lastIndex;
            lastHash = block.getHash().getBytes();
            result.add(block);
        }

        return result;
    }

    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }

    public static String padZeroesLeft(String s, int n) {
        return StringUtils.leftPad(s, n, '0');
    }
}
