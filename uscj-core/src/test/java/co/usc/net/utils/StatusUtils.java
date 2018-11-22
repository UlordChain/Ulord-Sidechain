package co.usc.net.utils;

import co.usc.net.Status;
import org.ethereum.core.Blockchain;

public class StatusUtils {
    public static Status fromBlockchain(Blockchain blockchain) {
        return new Status(
                blockchain.getBestBlock().getNumber(),
                blockchain.getBestBlockHash(),
                blockchain.getBestBlock().getParentHash().getBytes(),
                blockchain.getTotalDifficulty()
        );
    }

    public static Status getFakeStatus() {
        return new Status(0, null);
    }
}
