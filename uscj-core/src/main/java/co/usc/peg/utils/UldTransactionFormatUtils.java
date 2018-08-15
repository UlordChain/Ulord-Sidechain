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

import co.usc.ulordj.core.Sha256Hash;
import co.usc.ulordj.core.VarInt;

public class UldTransactionFormatUtils {
    private static int MIN_BLOCK_HEADER_SIZE = 140;
    private static int MAX_BLOCK_HEADER_SIZE = 145;

    public static Sha256Hash calculateUldTxHash(byte[] uldTxSerialized) {
        return Sha256Hash.wrapReversed(Sha256Hash.hashTwice(uldTxSerialized));
    }

    public static long getInputsCount(byte[] uldTxSerialized) {
        VarInt inputsCounter = new VarInt(uldTxSerialized, 4);
        return inputsCounter.value;
    }

    public static boolean isBlockHeaderSize(int size) {
        return size >= MIN_BLOCK_HEADER_SIZE && size <= MAX_BLOCK_HEADER_SIZE;
    }
}
