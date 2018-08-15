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

package co.usc.rpc;

import co.usc.mine.MinerWork;
import co.usc.mine.SubmittedBlockInfo;
import co.usc.rpc.modules.mnr.MnrModule;

public interface Web3MnrModule {

    default MinerWork mnr_getWork() {
        return getMnrModule().getWork();
    }

    default SubmittedBlockInfo mnr_submitUlordBlock(String ulordBlockHex) {
        return getMnrModule().submitUlordBlock(ulordBlockHex);
    }

    default SubmittedBlockInfo mnr_submitUlordBlockTransactions(
            String blockHashHex,
            String blockHeaderHex,
            String coinbaseHex,
            String txnHashesHex) {
        return getMnrModule().submitUlordBlockTransactions(blockHashHex, blockHeaderHex, coinbaseHex, txnHashesHex);
    }

    default SubmittedBlockInfo mnr_submitUlordBlockPartialMerkle(
            String blockHashHex,
            String blockHeaderHex,
            String coinbaseHex,
            String merkleHashesHex,
            String blockTxnCountHex) {
        return getMnrModule().submitUlordBlockPartialMerkle(blockHashHex, blockHeaderHex, coinbaseHex, merkleHashesHex, blockTxnCountHex);
    }

    MnrModule getMnrModule();
}
