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

package co.usc.core;

import com.google.common.primitives.UnsignedBytes;
import org.ethereum.rpc.TypeConverter;
import org.ethereum.vm.DataWord;
import org.bouncycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Immutable representation of an USC address.
 * It is a simple wrapper on the raw byte[].
 *
 * @author Ariel Mendelzon
 */
public class UscAddress {

    /**
     * This is the size of an USC address in bytes.
     */
    private static final int LENGTH_IN_BYTES = 20;

    private static final UscAddress NULL_ADDRESS = new UscAddress();

    /**
     * This compares using the lexicographical order of the sender unsigned bytes.
     */
    public static final Comparator<UscAddress> LEXICOGRAPHICAL_COMPARATOR = Comparator.comparing(
            UscAddress::getBytes,
            UnsignedBytes.lexicographicalComparator());

    private final byte[] bytes;

    /**
     * @param address a data word containing an address in the last 20 bytes.
     */
    public UscAddress(DataWord address) {
        this(address.getLast20Bytes());
    }

    /**
     * @param address the hex-encoded 20 bytes long address, with or without 0x prefix.
     */
    public UscAddress(String address) {
        this(TypeConverter.stringHexToByteArray(address));
    }

    /**
     * @param bytes the 20 bytes long raw address bytes.
     */
    public UscAddress(byte[] bytes) {
        if (bytes.length != LENGTH_IN_BYTES) {
            throw new RuntimeException(String.format("An USC address must be %d bytes long", LENGTH_IN_BYTES));
        }

        this.bytes = bytes;
    }

    /**
     * This instantiates the contract creation address.
     */
    private UscAddress() {
        this.bytes = new byte[0];
    }

    /**
     * @return the null address, which is the receiver of contract creation transactions.
     */
    public static UscAddress nullAddress() {
        return NULL_ADDRESS;
    }

    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }

        UscAddress otherSender = (UscAddress) other;
        return Arrays.equals(bytes, otherSender.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return Hex.toHexString(bytes);
    }
}
