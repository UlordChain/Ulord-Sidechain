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

import java.util.List;

/**
 * Builds Merkle proofs for inclusion in the merged-mining block header
 */
public interface MerkleProofBuilder {

    byte[] buildFromMerkleHashes(
            UldBlock blockWithHeaderOnly,
            List<String> merkleHashesString,
            int blockTxnCount);

    byte[] buildFromTxHashes(
            UldBlock blockWithHeaderOnly,
            List<String> txHashesString);

    byte[] buildFromBlock(UldBlock ulordMergedMiningBlock);
}
