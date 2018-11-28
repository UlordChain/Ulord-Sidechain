/*
 * This file is part of USC
 * Copyright (C) 2016 - 2018 USC developer team.
 * (derived from ethereumJ library, Copyright (c) 2016 <ether.camp>)
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

package org.ethereum.config;

import co.usc.config.UscSystemProperties;
import co.usc.core.DifficultyCalculator;
import co.usc.db.RepositoryImpl;
import co.usc.trie.TrieStoreImpl;
import org.ethereum.core.Repository;
import org.ethereum.core.Transaction;
import org.ethereum.datasource.KeyValueDataSource;
import org.ethereum.datasource.LevelDbDataSource;
import org.ethereum.db.TrieStorePoolOnDisk;
import org.ethereum.util.FileUtil;
import org.ethereum.validator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.ethereum.datasource.DataSourcePool.levelDbByName;

@Configuration
@ComponentScan(
        basePackages = { "org.ethereum", "co.usc" },
        excludeFilters = @ComponentScan.Filter(NoAutoscan.class))
public class CommonConfig {

    private static final Logger logger = LoggerFactory.getLogger("general");

    @Bean
    public Repository repository(UscSystemProperties config) {
        String databaseDir = config.databaseDir();
        if (config.databaseReset()){
            FileUtil.recursiveDelete(databaseDir);
            logger.info("Database reset done");
        }
        return buildRepository(databaseDir, config.detailsInMemoryStorageLimit());
    }

    public Repository buildRepository(String databaseDir, int memoryStorageLimit) {
        KeyValueDataSource ds = makeDataSource("state", databaseDir);
        KeyValueDataSource detailsDS = makeDataSource("details", databaseDir);

        return new RepositoryImpl(new TrieStoreImpl(ds), detailsDS,
                                  new TrieStorePoolOnDisk(databaseDir),
                                  memoryStorageLimit
        );
    }

    private KeyValueDataSource makeDataSource(String name, String databaseDir) {
        KeyValueDataSource ds = new LevelDbDataSource(name, databaseDir);
        ds.init();
        return ds;
    }

    @Bean
    public List<Transaction> transactionPoolTransactions() {
        return Collections.synchronizedList(new ArrayList<Transaction>());
    }

    @Bean
    public ParentBlockHeaderValidator parentHeaderValidator(UscSystemProperties config, DifficultyCalculator difficultyCalculator) {

        List<DependentBlockHeaderRule> rules = new ArrayList<>(asList(
                new ParentNumberRule(),
                new DifficultyRule(difficultyCalculator),
                new ParentGasLimitRule(config.getBlockchainConfig().
                        getCommonConstants().getGasLimitBoundDivisor())));

        return new ParentBlockHeaderValidator(rules);
    }

}
