/*
 * This file is part of USC
 * Copyright (C) 2016-2018 The Usc developers.
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

package co.usc;

import co.usc.config.UscSystemProperties;
import co.usc.core.UscAddress;
import co.usc.crypto.Keccak256;
import co.usc.trie.Trie;
import co.usc.trie.TrieImpl;
import co.usc.trie.TrieStore;
import co.usc.trie.TrieStoreImpl;
import org.bouncycastle.util.encoders.Hex;
import org.ethereum.config.DefaultConfig;
import org.ethereum.core.AccountState;
import org.ethereum.core.Block;
import org.ethereum.core.Blockchain;
import org.ethereum.core.Repository;
import org.ethereum.datasource.KeyValueDataSource;
import org.ethereum.util.BuildInfo;
import org.ethereum.vm.PrecompiledContracts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.ethereum.datasource.DataSourcePool.closeDataSource;
import static org.ethereum.datasource.DataSourcePool.levelDbByName;

@Component
public class DoPrune {
    private static Logger logger = LoggerFactory.getLogger("start");
    private static UscAddress DEFAULT_CONTRACT_ADDRESS = PrecompiledContracts.REMASC_ADDR;
    private static int DEFAULT_BLOCKS_TO_PROCESS = 5000;

    private final UscSystemProperties uscSystemProperties;
    private final Blockchain blockchain;

    public static void main(String[] args) {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(DefaultConfig.class);
        DoPrune runner = ctx.getBean(DoPrune.class);
        runner.doPrune();
        runner.stop();
    }

    @Autowired
    public DoPrune(
            UscSystemProperties uscSystemProperties,
            Blockchain blockchain) {
        this.uscSystemProperties = uscSystemProperties;
        this.blockchain = blockchain;
    }

    private void doPrune() {
        logger.info("Pruning Database");

        int blocksToProcess = DEFAULT_BLOCKS_TO_PROCESS;
        UscAddress contractAddress = DEFAULT_CONTRACT_ADDRESS;

        logger.info("Running {},  core version: {}-{}", uscSystemProperties.genesisInfo(), uscSystemProperties.projectVersion(), uscSystemProperties.projectVersionModifier());
        BuildInfo.printInfo();

        long height = this.blockchain.getBestBlock().getNumber();

        String dataSourceName = getDataSourceName(contractAddress);
        logger.info("Datasource Name {}", dataSourceName);
        logger.info("Blockchain height {}", height);

        TrieImpl source = new TrieImpl(new TrieStoreImpl(levelDbByName(dataSourceName, this.uscSystemProperties.databaseDir())), true);
        String targetDataSourceName = dataSourceName + "B";
        KeyValueDataSource targetDataSource = levelDbByName(targetDataSourceName, this.uscSystemProperties.databaseDir());
        TrieStore targetStore = new TrieStoreImpl(targetDataSource);

        this.processBlocks(height - blocksToProcess, source, contractAddress, targetStore);

        closeDataSource(targetDataSourceName);
    }

    private void processBlocks(long from, TrieImpl sourceTrie, UscAddress contractAddress, TrieStore targetStore) {
        long n = from;

        if (n <= 0) {
            n = 1;
        }

        while (true) {
            List<Block> blocks = this.blockchain.getBlocksByNumber(n);

            if (blocks.isEmpty()) {
                break;
            }

            for (Block b: blocks) {
                byte[] stateRoot = b.getStateRoot();

                logger.info("Block height {} State root {}", b.getNumber(), Hex.toHexString(stateRoot));
                Repository repo = this.blockchain.getRepository();
                repo.syncToRoot(stateRoot);
                logger.info("Repo root {}", Hex.toHexString(repo.getRoot()));

                AccountState accountState = repo.getAccountState(contractAddress);
                Keccak256 trieRoot = new Keccak256(accountState.getStateRoot());
                logger.info("Trie root {}", trieRoot);

                Trie contractStorage = sourceTrie.getSnapshotTo(trieRoot);
                contractStorage.copyTo(targetStore);
                logger.info("Trie root {}", contractStorage.getHash());
            }

            n++;
        }
    }

    public void stop() {
        logger.info("Shutting down USC node");
    }

    private static String getDataSourceName(UscAddress contractAddress) {
        return "details-storage/" + contractAddress;
    }
}

