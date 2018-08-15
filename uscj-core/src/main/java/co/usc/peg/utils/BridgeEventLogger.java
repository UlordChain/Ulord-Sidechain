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

package co.usc.peg.utils;

import co.usc.ulordj.core.UldECKey;
import co.usc.ulordj.core.UldTransaction;
import co.usc.peg.Federation;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.vm.LogInfo;

import java.util.List;

/**
 * Responsible for logging events triggered by BridgeContract.
 *
 * @author martin.medina
 */
public interface BridgeEventLogger {

    void logUpdateCollections(Transaction uscTx);

    void logAddSignature(UldECKey federatorPublicKey, UldTransaction uldTx, byte[] uscTxHash);

    void logReleaseUld(UldTransaction uldTx);

    void logCommitFederation(Block executionBlock, Federation oldFederation, Federation newFederation);
}
