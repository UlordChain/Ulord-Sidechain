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

package co.usc.trie;

import org.ethereum.datasource.HashMapDB;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by ajlopez on 03/04/2017.
 */
public class SecureTrieImplSaveRetrieveTest {
    @Test
    public void updateSaveRetrieveAndGetOneThousandKeyValues() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(store, true);

        for (int k = 0; k < 1000; k++)
            trie = trie.put(k + "", (k + "").getBytes());

        trie.save();

        Trie trie2 = store.retrieve(trie.getHash().getBytes());

        Assert.assertNotNull(trie2);
        Assert.assertEquals(trie.getHash(), trie2.getHash());

        for (int k = 0; k < 1000; k++) {
            String key = k + "";
            byte[] expected = trie.get(key);
            byte[] value = trie2.get(key);
            Assert.assertArrayEquals(expected, value);
        }
    }

    @Test
    public void updateSaveRetrieveAndGetOneThousandKeyLongValues() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(store, true);

        for (int k = 0; k < 1000; k++)
            trie = trie.put(k + "", TrieImplValueTest.makeValue(k + 200));

        trie.save();

        Trie trie2 = store.retrieve(trie.getHash().getBytes());

        Assert.assertNotNull(trie2);
        Assert.assertEquals(trie.getHash(), trie2.getHash());

        for (int k = 0; k < 1000; k++) {
            String key = k + "";
            byte[] expectedValue = trie.get(key);
            byte[] value = trie2.get(key);
            Assert.assertArrayEquals(expectedValue, value);
        }
    }

    @Test
    public void updateSaveRetrieveAndGetOneThousandKeyValuesUsingBinaryTree() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(store, true);

        for (int k = 0; k < 1000; k++)
            trie = trie.put(k + "", (k + "").getBytes());

        trie.save();

        Trie trie2 = store.retrieve(trie.getHash().getBytes());

        Assert.assertNotNull(trie2);
        Assert.assertEquals(trie.getHash(), trie2.getHash());

        for (int k = 0; k < 1000; k++) {
            String key = k + "";
            byte[] expected = trie.get(key);
            byte[] value = trie2.get(key);
            Assert.assertArrayEquals(expected, value);
        }
    }

    @Test
    public void updateSaveRetrieveAndGetOneThousandKeyLongValuesUsingBinaryTree() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(store, true);

        for (int k = 0; k < 1000; k++)
            trie = trie.put(k + "", TrieImplValueTest.makeValue(k + 200));

        trie.save();

        Trie trie2 = store.retrieve(trie.getHash().getBytes());

        Assert.assertNotNull(trie2);
        Assert.assertEquals(trie.getHash(), trie2.getHash());

        for (int k = 0; k < 1000; k++) {
            String key = k + "";
            byte[] expected = trie.get(key);
            byte[] value = trie2.get(key);
            Assert.assertArrayEquals(expected, value);
        }
    }

    @Test
    public void updateSaveRetrieveAndGetOneThousandKeyValuesInverseOrder() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(store, true);

        for (int k = 1000; k > 0; k--)
            trie = trie.put(k + "", (k + "").getBytes());

        trie.save();

        Trie trie2 = store.retrieve(trie.getHash().getBytes());

        Assert.assertNotNull(trie2);
        Assert.assertEquals(trie.getHash(), trie2.getHash());

        for (int k = 1000; k > 0; k--) {
            String key = k + "";
            byte[] expected = trie.get(key);
            byte[] value = trie2.get(key);
            Assert.assertArrayEquals(expected, value);
        }
    }

    @Test
    public void updateSaveRetrieveAndGetOneThousandKeyLongValuesInverseOrder() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(store, true);

        for (int k = 1000; k > 0; k--)
            trie = trie.put(k + "", TrieImplValueTest.makeValue(k + 200));

        trie.save();

        Trie trie2 = store.retrieve(trie.getHash().getBytes());

        Assert.assertNotNull(trie2);
        Assert.assertEquals(trie.getHash(), trie2.getHash());

        for (int k = 1000; k > 0; k--) {
            String key = k + "";
            byte[] expected = trie.get(key);
            byte[] value = trie2.get(key);
            Assert.assertArrayEquals(expected, value);
        }
    }

    @Test
    public void saveTrieWithKeyValues() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(store, true)
                .put("foo", "bar".getBytes())
                .put("bar", "baz".getBytes())
                .put("answer", "42".getBytes());

        trie.save();

        Assert.assertNotEquals(0, trie.trieSize());
        Assert.assertEquals(trie.trieSize(), map.keys().size());
    }

    @Test
    public void saveTrieWithKeyLongValues() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(store, true)
                .put("foo", "bar".getBytes())
                .put("bar", TrieImplValueTest.makeValue(100))
                .put("answer", TrieImplValueTest.makeValue(200));

        trie.save();

        Assert.assertNotEquals(0, trie.trieSize());
        Assert.assertEquals(trie.trieSize() + 2, map.keys().size());
    }

    @Test
    public void retrieveTrieUsingHash() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(store, true)
                .put("foo", "bar".getBytes())
                .put("bar", "baz".getBytes())
                .put("answer", "42".getBytes());

        trie.save();

        Trie trie2 = store.retrieve(trie.getHash().getBytes());

        Assert.assertNotNull(trie2);
        Assert.assertEquals(trie.trieSize(), trie2.trieSize());
        Assert.assertEquals(trie.getHash(), trie2.getHash());

        Assert.assertArrayEquals(trie.get("foo"), trie2.get("foo"));
        Assert.assertArrayEquals(trie.get("bar"), trie2.get("bar"));
        Assert.assertArrayEquals(trie.get("answer"), trie2.get("answer"));
    }

    @Test
    public void retrieveTrieWithLongValuesUsingHash() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(store, true)
                .put("foo", "bar".getBytes())
                .put("bar", TrieImplValueTest.makeValue(100))
                .put("answer", TrieImplValueTest.makeValue(200));

        trie.save();

        Trie trie2 = store.retrieve(trie.getHash().getBytes());

        Assert.assertNotNull(trie2);
        Assert.assertEquals(trie.trieSize(), trie2.trieSize());
        Assert.assertEquals(trie.getHash(), trie2.getHash());

        Assert.assertArrayEquals(trie.get("foo"), trie2.get("foo"));
        Assert.assertArrayEquals(trie.get("bar"), trie2.get("bar"));
        Assert.assertArrayEquals(trie.get("answer"), trie2.get("answer"));
    }
}
