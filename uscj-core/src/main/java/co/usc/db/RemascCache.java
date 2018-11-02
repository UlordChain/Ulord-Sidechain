package co.usc.db;

import co.usc.crypto.Keccak256;
import co.usc.remasc.Sibling;

import java.util.List;
import java.util.Map;

public interface RemascCache {

    Map<Long, List<Sibling>> getSiblingsFromBlockByHash(Keccak256 hash);
}
