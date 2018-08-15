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

package co.usc.mine;

import co.usc.ulordj.core.UldBlock;
import co.usc.ulordj.core.UldTransaction;
import co.usc.core.UscAddress;
import org.ethereum.core.Block;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;


public interface MinerServer {

    void start();

    void stop();

    boolean isRunning();

    SubmitBlockResult submitUlordBlockPartialMerkle(
            String blockHashForMergedMining,
            UldBlock blockWithOnlyHeader,
            UldTransaction coinbase,
            List<String> merkleHashes,
            int blockTxnCount
    );

    SubmitBlockResult submitUlordBlockTransactions(
            String blockHashForMergedMining,
            UldBlock blockWithOnlyHeader,
            UldTransaction coinbase,
            List<String> txHashes
    );

    SubmitBlockResult submitUlordBlock(String blockHashForMergedMining, UldBlock ulordMergedMiningBlock);

    boolean generateFallbackBlock();

    void setFallbackMining(boolean p);

    void setAutoSwitchBetweenNormalAndFallbackMining(boolean p);

    boolean isFallbackMining();

    UscAddress getCoinbaseAddress();

    MinerWork getWork();

    void buildBlockToMine(@Nonnull Block newParent, boolean createCompetitiveBlock);

    void setExtraData(byte[] extraData);

    long getCurrentTimeInSeconds();

    long increaseTime(long seconds);

    Optional<Block> getLatestBlock();
}
