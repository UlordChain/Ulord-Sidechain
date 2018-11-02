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

package co.usc.db;

import co.usc.config.UscSystemProperties;
import co.usc.core.UscAddress;
import co.usc.trie.TrieCopier;
import co.usc.trie.TrieStore;
import co.usc.trie.TrieStoreImpl;
import org.ethereum.core.Blockchain;
import org.ethereum.datasource.KeyValueDataSource;
import org.ethereum.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ethereum.datasource.DataSourcePool.levelDbByName;
import static org.ethereum.datasource.DataSourcePool.closeDataSource;

/**
 * Created by ajlopez on 21/03/2018.
 */
public class PruneService {
    private static final Logger logger = LoggerFactory.getLogger("prune");

    private final UscSystemProperties uscConfiguration;
    private final PruneConfiguration pruneConfiguration;
    private final Blockchain blockchain;
    private final UscAddress contractAddress;

    private boolean stopped;
    private long nextBlockNumber;

    public PruneService(PruneConfiguration pruneConfiguration, UscSystemProperties uscConfiguration, Blockchain blockchain, UscAddress contractAddress) {
        this.pruneConfiguration = pruneConfiguration;
        this.uscConfiguration = uscConfiguration;
        this.blockchain = blockchain;
        this.contractAddress = contractAddress;
        this.nextBlockNumber = pruneConfiguration.getNoBlocksToWait();
    }

    public void start() {
        this.stopped = false;
        new Thread(this::run).start();
        logger.info("launched");
    }

    public void run() {
        while (this.stopped == false) {
            long bestBlockNumber = this.blockchain.getStatus().getBestBlockNumber();

            if (bestBlockNumber > nextBlockNumber) {
                logger.info("Starting prune at height {}", bestBlockNumber);

                try {
                    this.process();
                }
                catch (Throwable t) {
                    logger.error("Error {}", t.getMessage());
                }

                logger.info("Prune done");

                nextBlockNumber = this.blockchain.getStatus().getBestBlockNumber() + this.pruneConfiguration.getNoBlocksToWait();
            }

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                logger.error("Interrupted {}", e.getMessage());
            }
        }
    }

    public void stop() {
        this.stopped = true;
    }

    public void process() {
        long from = this.blockchain.getBestBlock().getNumber() - this.pruneConfiguration.getNoBlocksToCopy();
        long to = this.blockchain.getBestBlock().getNumber() - this.pruneConfiguration.getNoBlocksToAvoidForks();

        String dataSourceName = getDataSourceName(contractAddress);
        KeyValueDataSource sourceDataSource = levelDbByName(dataSourceName, this.uscConfiguration.databaseDir());
        KeyValueDataSource targetDataSource = levelDbByName(dataSourceName + "B", this.uscConfiguration.databaseDir());
        TrieStore targetStore = new TrieStoreImpl(targetDataSource);

        TrieCopier.trieContractStateCopy(targetStore, blockchain, from, to, blockchain.getRepository(), this.contractAddress);

        long to2 = this.blockchain.getBestBlock().getNumber() - this.pruneConfiguration.getNoBlocksToAvoidForks();

        TrieCopier.trieContractStateCopy(targetStore, blockchain, to, to2, blockchain.getRepository(), this.contractAddress);

        blockchain.suspendProcess();

        logger.info("Suspend blockchain process");

        try {
            TrieCopier.trieContractStateCopy(targetStore, blockchain, to2, 0, blockchain.getRepository(), this.contractAddress);

            closeDataSource(dataSourceName);
            targetDataSource.close();
            sourceDataSource.close();

            String contractDirectoryName = getDatabaseDirectory(uscConfiguration, dataSourceName);

            removeDirectory(contractDirectoryName);

            boolean result = FileUtil.fileRename(contractDirectoryName + "B", contractDirectoryName);

            if (!result) {
                logger.error("Unable to rename contract storage");
            }

            sourceDataSource.init();
            //levelDbByName(this.uscConfiguration, dataSourceName);
        }
        finally {
            blockchain.resumeProcess();

            logger.info("Resume blockchain process");
        }
    }

    private static String getDatabaseDirectory(UscSystemProperties config, String subdirectoryName) {
        return FileUtil.getDatabaseDirectoryPath(config.databaseDir(), subdirectoryName).toString();
    }

    private static String getDataSourceName(UscAddress contractAddress) {
        return "details-storage/" + contractAddress;
    }

    private static void removeDirectory(String directoryName) {
        FileUtil.recursiveDelete(directoryName);
    }
}

