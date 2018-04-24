package co.usc.trie;

import co.usc.crypto.Keccak256;
import org.ethereum.crypto.HashUtil;
import org.ethereum.util.RLP;
import org.junit.Assert;
import org.junit.Test;

import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;

/**
 * Created by ajlopez on 06/05/2017.
 */
public class TrieImplDeleteTest {
    @Test
    public void deleteValueGivintEmptyTrie() {
        Trie trie = new TrieImpl().put("key", "value".getBytes());

        trie = trie.delete("key".getBytes());

        Assert.assertEquals(makeEmptyHash(), trie.getHash());
    }

    @Test
    public void deleteOneValueGivesTheSameHash() {
        Trie trie1 = new TrieImpl()
                .put("key1", "value1".getBytes())
                .put("key2", "value2".getBytes())
                .delete("key1");

        Trie trie2 = new TrieImpl().put("key2", "value2".getBytes());

        Assert.assertEquals(trie1.getHash(), trie2.getHash());
    }

    @Test
    public void deleteOneLongValueGivesTheSameHash() {
        Trie trie1 = new TrieImpl()
                .put("key1", TrieImplValueTest.makeValue(1024))
                .put("key2", "value2".getBytes())
                .delete("key1");

        Trie trie2 = new TrieImpl().put("key2", "value2".getBytes());

        Assert.assertEquals(trie1.getHash(), trie2.getHash());
    }

    @Test
    public void deleteOneValueTwiceGivesTheSameHash() {
        Trie trie1 = new TrieImpl()
                .put("key1", "value1".getBytes())
                .put("key2", "value2".getBytes())
                .put("key2", "value2".getBytes())
                .delete("key1");

        Trie trie2 = new TrieImpl().put("key2", "value2".getBytes());

        Assert.assertEquals(trie1.getHash(), trie2.getHash());
    }

    @Test
    public void deleteOneHundredValuesGivesTheSameHash() {
        Trie trie1 = new TrieImpl();

        for (int k = 0; k < 200; k++)
            trie1 = trie1.put("key" + k, ("value" + k).getBytes());

        for (int k = 1; k < 200; k += 2)
            trie1 = trie1.delete("key" + k);

        Trie trie2 = new TrieImpl();

        for (int k = 0; k < 200; k += 2)
            trie2 = trie2.put("key" + k, ("value" + k).getBytes());

        Assert.assertEquals(trie1.getHash(), trie2.getHash());
    }

    @Test
    public void deleteOneHundredLongValuesGivesTheSameHash() {
        Trie trie1 = new TrieImpl();

        for (int k = 0; k < 200; k++)
            trie1 = trie1.put("key" + k, TrieImplValueTest.makeValue(k + 100));

        for (int k = 1; k < 200; k += 2)
            trie1 = trie1.delete("key" + k);

        Trie trie2 = new TrieImpl();

        for (int k = 0; k < 200; k += 2)
            trie2 = trie2.put("key" + k, TrieImplValueTest.makeValue(k + 100));

        Assert.assertEquals(trie1.getHash(), trie2.getHash());
    }

    @Test
    public void deleteOneHundredValuesGivesTheSameHashUsingSecureKeys() {
        Trie trie1 = new TrieImpl(true);

        for (int k = 0; k < 200; k++)
            trie1 = trie1.put("key" + k, ("value" + k).getBytes());

        for (int k = 1; k < 200; k += 2)
            trie1 = trie1.delete("key" + k);

        Trie trie2 = new TrieImpl(true);

        for (int k = 0; k < 200; k += 2)
            trie2 = trie2.put("key" + k, ("value" + k).getBytes());

        Assert.assertEquals(trie1.getHash(), trie2.getHash());
    }

    @Test
    public void deleteOneHundredLongValuesGivesTheSameHashUsingSecureKeys() {
        Trie trie1 = new TrieImpl(true);

        for (int k = 0; k < 200; k++)
            trie1 = trie1.put("key" + k, TrieImplValueTest.makeValue(k + 200));

        for (int k = 1; k < 200; k += 2)
            trie1 = trie1.delete("key" + k);

        Trie trie2 = new TrieImpl(true);

        for (int k = 0; k < 200; k += 2)
            trie2 = trie2.put("key" + k, TrieImplValueTest.makeValue(k + 200));

        Assert.assertEquals(trie1.getHash(), trie2.getHash());
    }

    @Test
    public void deleteTwoHundredValuesGivesTheEmptyHash() {
        Trie trie1 = new TrieImpl();

        for (int k = 0; k < 200; k++)
            trie1 = trie1.put("key" + k, ("value" + k).getBytes());

        for (int k = 0; k < 200; k++)
            trie1 = trie1.delete("key" + k);

        Assert.assertEquals(makeEmptyHash(), trie1.getHash());
    }

    @Test
    public void deleteTwoHundredLongValuesGivesTheEmptyHash() {
        Trie trie1 = new TrieImpl();

        for (int k = 0; k < 200; k++)
            trie1 = trie1.put("key" + k, TrieImplValueTest.makeValue(k + 200));

        for (int k = 0; k < 200; k++)
            trie1 = trie1.delete("key" + k);

        Assert.assertEquals(makeEmptyHash(), trie1.getHash());
    }

    @Test
    public void deleteOneHundredAndOneHundredValuesGivesTheEmptyHash() {
        Trie trie1 = new TrieImpl();

        for (int k = 0; k < 200; k++)
            trie1 = trie1.put("key" + k, ("value" + k).getBytes());

        for (int k = 0; k < 200; k += 2)
            trie1 = trie1.delete("key" + k);

        for (int k = 1; k < 200; k += 2)
            trie1 = trie1.delete("key" + k);

        Assert.assertEquals(makeEmptyHash(), trie1.getHash());
    }

    @Test
    public void deleteOneHundredAndOneHundredLongValuesGivesTheEmptyHash() {
        Trie trie1 = new TrieImpl();

        for (int k = 0; k < 200; k++)
            trie1 = trie1.put("key" + k, TrieImplValueTest.makeValue(k + 200));

        for (int k = 0; k < 200; k += 2)
            trie1 = trie1.delete("key" + k);

        for (int k = 1; k < 200; k += 2)
            trie1 = trie1.delete("key" + k);

        Assert.assertEquals(makeEmptyHash(), trie1.getHash());
    }

    @Test
    public void deleteOneHundredAndOneHundredValuesGivesTheEmptyHashUsingSecureKeys() {
        Trie trie1 = new TrieImpl(true);

        for (int k = 0; k < 200; k++)
            trie1 = trie1.put("key" + k, ("value" + k).getBytes());

        for (int k = 0; k < 200; k += 2)
            trie1 = trie1.delete("key" + k);

        for (int k = 1; k < 200; k += 2)
            trie1 = trie1.delete("key" + k);

        Assert.assertEquals(makeEmptyHash(), trie1.getHash());
    }

    @Test
    public void deleteOneHundredAndOneHundredLongValuesGivesTheEmptyHashUsingSecureKeys() {
        Trie trie1 = new TrieImpl(true);

        for (int k = 0; k < 200; k++)
            trie1 = trie1.put("key" + k, TrieImplValueTest.makeValue(k + 200));

        for (int k = 0; k < 200; k += 2)
            trie1 = trie1.delete("key" + k);

        for (int k = 1; k < 200; k += 2)
            trie1 = trie1.delete("key" + k);

        Assert.assertEquals(makeEmptyHash(), trie1.getHash());
    }

    public static Keccak256 makeEmptyHash() {
        return new Keccak256(HashUtil.keccak256(RLP.encodeElement(EMPTY_BYTE_ARRAY)));
    }
}
