package co.usc.db;

import co.usc.trie.TrieStore;
import co.usc.trie.TrieStoreImpl;
import org.ethereum.datasource.HashMapDB;
import org.ethereum.datasource.KeyValueDataSource;

import java.util.HashMap;
import java.util.Map;

public class TrieStorePoolOnMemory implements TrieStore.Pool {

    private final StoreProvider storeProvider;
    Map<String, TrieStore> pool = new HashMap<>();

    public TrieStorePoolOnMemory() {
        this (HashMapDB::new);
    }

    public TrieStorePoolOnMemory(StoreProvider storeProvider) {
        this.storeProvider = storeProvider;
    }

    @Override
    public TrieStore getInstanceFor(String name) {
        return pool.computeIfAbsent(name, trieStoreName -> new TrieStoreImpl(storeProvider.getInstance()));
    }

    public interface StoreProvider {
        KeyValueDataSource getInstance();
    }
}
