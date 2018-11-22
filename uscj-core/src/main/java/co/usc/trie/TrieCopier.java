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

import co.usc.core.UscAddress;
import co.usc.crypto.Keccak256;
import co.usc.db.ContractDetailsImpl;
import org.ethereum.core.AccountState;
import org.ethereum.core.Block;
import org.ethereum.core.Blockchain;
import org.ethereum.core.Repository;

import java.util.List;

/**
 * Created by ajlopez on 09/03/2018.
 */
public class TrieCopier {
    private TrieCopier() {

    }

    public static void trieStateCopy(TrieStore source, TrieStore target, Keccak256 hash) {
        Trie trie = source.retrieve(hash.getBytes());
        trie.copyTo(target);
    }

    public static void trieStateCopy(TrieStore source, TrieStore target, Blockchain blockchain, int initialHeight) {
        long h = initialHeight;

        List<Block> blocks = blockchain.getBlocksByNumber(h);

        while (!blocks.isEmpty()) {
            for (Block block : blocks) {
                trieStateCopy(source, target, new Keccak256(block.getStateRoot()));
            }

            h++;
            blocks = blockchain.getBlocksByNumber(h);
        }
    }

    public static void trieContractStateCopy(TrieStore target, Blockchain blockchain, long initialHeight, long finalHeight, Repository repository, UscAddress contractAddress) {
        long h = initialHeight;

        List<Block> blocks = blockchain.getBlocksByNumber(h);

        while (!blocks.isEmpty()) {
            for (Block block : blocks) {
                Repository stateRepository = repository.getSnapshotTo(block.getStateRoot());
                AccountState accountState = stateRepository.getAccountState(contractAddress);

                ContractDetailsImpl contractDetails = (ContractDetailsImpl)stateRepository.getContractDetails(contractAddress);
                TrieImpl trie = (TrieImpl)contractDetails.getTrie();
                trieStateCopy(trie.getStore(), target, new Keccak256(accountState.getStateRoot()));
            }

            h++;

            if (finalHeight > 0 && finalHeight < h) {
                break;
            }

            blocks = blockchain.getBlocksByNumber(h);
        }
    }
}
