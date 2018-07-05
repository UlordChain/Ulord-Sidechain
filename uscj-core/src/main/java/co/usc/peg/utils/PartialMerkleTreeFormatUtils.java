/*
 * This file is part of USC
 * Copyright (C) 2016 - 2018 Usc Development team.
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

import co.usc.ulordj.core.Sha256Hash;
import co.usc.ulordj.core.VarInt;

public class PartialMerkleTreeFormatUtils {

    private static final int BLOCK_TRANSACTION_COUNT_LENGTH = 4;

    public static VarInt getHashesCount(byte[] pmtSerialized) {
        return new VarInt(pmtSerialized, BLOCK_TRANSACTION_COUNT_LENGTH);
    }

    public static VarInt getFlagBitsCount(byte[] pmtSerialized) {
        VarInt hashesCount = getHashesCount(pmtSerialized);
        return new VarInt(
                pmtSerialized,
                Math.addExact(
                    BLOCK_TRANSACTION_COUNT_LENGTH + hashesCount.getOriginalSizeInBytes(),
                    Math.multiplyExact(Math.toIntExact(hashesCount.value), Sha256Hash.LENGTH)
                )
        );
    }

    public static boolean hasExpectedSize(byte[] pmtSerialized) {
        try {
            VarInt hashesCount = getHashesCount(pmtSerialized);
            VarInt flagBitsCount = getFlagBitsCount(pmtSerialized);
            int declaredSize = Math.addExact(Math.addExact(BLOCK_TRANSACTION_COUNT_LENGTH
                    + hashesCount.getOriginalSizeInBytes()
                    + flagBitsCount.getOriginalSizeInBytes(),
                    Math.toIntExact(flagBitsCount.value)),
                    Math.multiplyExact(Math.toIntExact(hashesCount.value), Sha256Hash.LENGTH)
            );
            return pmtSerialized.length == declaredSize;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
