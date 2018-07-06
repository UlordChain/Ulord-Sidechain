/*
 * This file is part of Usc
 * Copyright (c) 2016 - 2018 Ulord development team.
 */

package tools;

import co.usc.ulordj.core.*;
import co.usc.ulordj.core.Utils;

import java.util.ArrayList;
import java.util.List;

public class GenerateMerkleTree {
    // The Merkle root is based on a tree of hashes calculated from the transactions:
    //
    //     root
    //      / \
    //   A      B
    //  / \    / \
    // t1 t2 t3 t4
    //
    // The tree is represented as a list: t1,t2,t3,t4,A,B,root where each
    // entry is a hash.

    public static PartialMerkleTree buildMerkleBranch(NetworkParameters params, UldBlock block, Sha256Hash txToInclude) {

        List<UldTransaction> txs = block.getTransactions();
        List<Sha256Hash> txHashes = new ArrayList<>(txs.size());

        for (UldTransaction tx : txs) {
            txHashes.add(tx.getHash());
        }

        int index = 0;
        for(Sha256Hash hash : txHashes) {
            if (hash.equals(txToInclude))
                break;
            index ++;
        }

        /*
           We need to convert the txs to a bitvector to choose which ones
           will be included in the Partial Merkle Tree.

           We need txs.size() / 8 bytes to represent this vector.
           The coinbase tx is the first one of the txs so we set the first bit to 1.
         */
        byte[] bitvector = new byte[(txHashes.size() + 7) / 8];
        Utils.setBitLE(bitvector, index);
        return PartialMerkleTree.buildFromLeaves(params, bitvector, txHashes);
    }
}
