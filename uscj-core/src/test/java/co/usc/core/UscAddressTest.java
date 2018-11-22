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

import org.junit.Assert;
import org.junit.Test;
import org.bouncycastle.util.encoders.DecoderException;

public class UscAddressTest {
    @Test
    public void testEquals() {
        UscAddress senderA = new UscAddress("0000000000000000000000000000000001000006");
        UscAddress senderB = new UscAddress("0000000000000000000000000000000001000006");
        UscAddress senderC = new UscAddress("0000000000000000000000000000000001000008");
        UscAddress senderD = UscAddress.nullAddress();
        UscAddress senderE = new UscAddress("0x00002000f000000a000000330000000001000006");

        Assert.assertEquals(senderA, senderB);
        Assert.assertNotEquals(senderA, senderC);
        Assert.assertNotEquals(senderA, senderD);
        Assert.assertNotEquals(senderA, senderE);
    }

    @Test
    public void nullAddress() {
        UscAddress senderA = new UscAddress("0000000000000000000000000000000000000000");
        UscAddress senderB = new UscAddress("0x0000000000000000000000000000000000000000");
        UscAddress senderC = new UscAddress(new byte[20]);
        UscAddress senderD = new UscAddress("0000000000000000000000000000000000000001");

        Assert.assertEquals(UscAddress.nullAddress(), senderA);
        Assert.assertEquals(UscAddress.nullAddress(), senderB);
        Assert.assertEquals(UscAddress.nullAddress(), senderC);
        Assert.assertNotEquals(UscAddress.nullAddress(), senderD);
    }

    @Test(expected = RuntimeException.class)
    public void invalidLongAddress() {
        new UscAddress("00000000000000000000000000000000010000060");
    }

    @Test(expected = RuntimeException.class)
    public void invalidShortAddress() {
        new UscAddress("0000000000000000000000000000000001006");
    }

    @Test
    public void oddLengthAddressPaddedWithOneZero() {
        new UscAddress("000000000000000000000000000000000100006");
    }

    @Test(expected = DecoderException.class)
    public void invalidHexAddress() {
        new UscAddress("000000000000000000000000000000000100000X");
    }

    @Test(expected = NullPointerException.class)
    public void invalidNullAddressBytes() {
        new UscAddress((byte[]) null);
    }

    @Test(expected = NullPointerException.class)
    public void invalidNullAddressString() {
        new UscAddress((String) null);
    }

    @Test(expected = RuntimeException.class)
    public void invalidShortAddressBytes() {
        new UscAddress(new byte[19]);
    }

    @Test(expected = RuntimeException.class)
    public void invalidLongAddressBytes() {
        new UscAddress(new byte[21]);
    }

}
