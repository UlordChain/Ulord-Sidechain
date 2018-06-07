/*
 * This file is part of Usc
 * Copyright (c) 2016 - 2018 Ulord development team.
 */

package tools;

import co.usc.ulordj.core.*;
import co.usc.ulordj.params.TestNet3Params;
import com.sun.istack.internal.NotNull;
import org.spongycastle.util.test.Test;

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

    private static final NetworkParameters params = TestNet3Params.get();
    public static void main(@NotNull String[] args) {
        if(args.length < 2) {
            System.out.println("args: <serialized block in hex> <transaction id to be included>");
            return;
        }

        UldBlock block = new UldBlock(params , Sha256Hash.hexStringToByteArray(args[0]));
        Sha256Hash txToInclude = new Sha256Hash(args[1]);
        List<UldTransaction> txs = block.getTransactions();
        List<Sha256Hash> txHashes = new ArrayList<>(txs.size());

        for (UldTransaction tx : txs) {
            txHashes.add(tx.getHash());
        }

        PartialMerkleTree tree = buildMerkleBranch(txHashes, txToInclude, block.getParams());
        byte[] treebytes = tree.ulordSerialize();

        System.out.println(Sha256Hash.bytesToHex(treebytes).toLowerCase());
    }

    private static PartialMerkleTree buildMerkleBranch(List<Sha256Hash> txHashes, Sha256Hash txToInclude, NetworkParameters params) {

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