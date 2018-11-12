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

package org.ethereum.config.blockchain;

import co.usc.core.BlockDifficulty;
import org.ethereum.config.BlockchainConfig;
import org.ethereum.config.BlockchainNetConfig;
import org.ethereum.config.Constants;
import org.ethereum.config.blockchain.testnet.TestNetAfterBridgeSyncConfig;
import org.ethereum.core.BlockHeader;
import java.math.BigInteger;

import static org.ethereum.util.BIUtil.max;

/**
 * BlockchainForkConfig is also implemented by this class - its (mostly testing) purpose to represent
 * the specific config for all blocks on the chain (kinda constant config).
 *
 * Created by Anton Nashatyrev on 25.02.2016.
 */
public abstract class AbstractConfig implements BlockchainConfig, BlockchainNetConfig {
    protected Constants constants;

    public AbstractConfig() {
        this(new Constants());
    }

    public AbstractConfig(Constants constants) {
        this.constants = constants;
    }

    @Override
    public Constants getConstants() {
        return constants;
    }

    @Override
    public BlockchainConfig getConfigForBlock(long blockHeader) {
        return this;
    }

    @Override
    public Constants getCommonConstants() {
        return getConstants();
    }


    @Override
    public BlockDifficulty calcDifficulty(BlockHeader curBlockHeader, BlockHeader parent) {
        BlockDifficulty pd = parent.getDifficulty();
        int uncleCount = curBlockHeader.getUncleCount();
        long curBlockTS = curBlockHeader.getTimestamp();
        long parentBlockTS =parent.getTimestamp();

        return calcDifficultyFortConstants(getConstants(),curBlockTS, parentBlockTS,pd,uncleCount);
    }

    public BlockDifficulty getBlockDifficulty(Constants constants, BlockHeader curBlockHeader, BlockHeader parent) {
        BlockDifficulty pd = parent.getDifficulty();
        int uncleCount = curBlockHeader.getUncleCount();
        long curBlockTS = curBlockHeader.getTimestamp();
        long parentBlockTS = parent.getTimestamp();

        return calcDifficultyFortConstants(constants, curBlockTS, parentBlockTS, pd, uncleCount);
    }

    public static BlockDifficulty calcDifficultyFortConstants(Constants constants,
                                                         long curBlockTS,
                                                         long parentBlockTS,
                                                         BlockDifficulty pd ,
                                                         int uncleCount) {
        int duration =constants.getDurationLimit();

        // Created Fork to reduce difficulty variation to 2% for testnet
        // Friday, August 17, 2018 10:00:00 AM GMT+08:00
        BigInteger difDivisor;
        if(constants instanceof TestNetAfterBridgeSyncConfig.TestNetConstants) {
            if(curBlockTS < 1534471200L)
                difDivisor = constants.getDifficultyBoundDivisor();
            else
                difDivisor = BigInteger.valueOf(50);
        } else {
            difDivisor = constants.getDifficultyBoundDivisor();
        }

        BlockDifficulty minDif = constants.getMinimumDifficulty();
        return calcDifficultyWithTimeStamps(constants, curBlockTS, parentBlockTS,pd,uncleCount,duration,difDivisor,minDif );
    }

    public static BlockDifficulty calcDifficultyWithTimeStamps(Constants constants, long curBlockTS, long parentBlockTS,
                                                               BlockDifficulty pd, int uncleCount, int duration,
                                                               BigInteger difDivisor,
                                                               BlockDifficulty minDif ) {

        long delta = curBlockTS-parentBlockTS;
        if (delta<0) {
            return pd;
        }

        int calcDur;

        // Created Fork to reduce time 25% as Bitcoin and Ulord time difference is 25%
        // Monday, August 20, 2018 11:00:00 AM GMT+08:00
        if(constants instanceof TestNetAfterBridgeSyncConfig.TestNetConstants) {
            if(curBlockTS > 1534734000L) {
                calcDur = (int)((1 + uncleCount * 0.175) * duration);
            } else {
                calcDur = (1 + uncleCount) * duration;
            }
        } else {
            calcDur = (int)((1 + uncleCount * 0.175) * duration);
        }

        int sign;
        if (calcDur>delta) {
            sign =1;
        }else if (calcDur<delta) { 
            sign =-1;
        }else{// (calcDur == delta), and then (sign==0) 
            return pd;
        }

        BigInteger pdValue = pd.asBigInteger();
        BigInteger quotient = pdValue.divide(difDivisor);

        BigInteger fromParent;
        if (sign==1) {
            fromParent =pdValue.add(quotient);
        } else {
            fromParent =pdValue.subtract(quotient);
        }

        // If parent difficulty is zero (maybe a genesis block), then the first child difficulty MUST
        // be greater or equal getMinimumDifficulty(). That's why the max() is applied in both the add and the sub
        // cases.
        // Note that we have to apply max() first in case fromParent ended up being negative.
        return new BlockDifficulty(max(minDif.asBigInteger(), fromParent));
    }

    protected abstract BigInteger getCalcDifficultyMultiplier(BlockHeader curBlock, BlockHeader parent);

    @Override
    public boolean areBridgeTxsFree() {
        return false;
    }

    @Override
    public boolean isUscIP85() { return false; }

    @Override
    public boolean isUscIP87() { return false; }

    @Override
    public boolean isUscIP88() {
        return false;
    }

    @Override
    public boolean isUscIP89() {
        return false;
    }

    @Override
    public boolean isUscIP90() {
        return false;
    }

    @Override
    public boolean isUscIP91() {
        return false;
    }

    @Override
    public boolean isUscIP92() {return false; }

    @Override
    public boolean isUscIP93() {return false; }

    @Override
    public boolean isUscIP94() {
        return false;
    }

    @Override
    public boolean isUscIP98() {
        return false;
    }
}
