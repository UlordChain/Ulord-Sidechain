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

import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class PartialMerkleTreeFormatUtilsTest {

    @Test
    public void getHashesCount() {
        String pmtSerializedEncoded = "030000000279e7c0da739df8a00f12c0bff55e5438f530aa5859ff9874258cd7bad3fe709746aff89" +
                                      "7e6a851faa80120d6ae99db30883699ac0428fc7192d6c3fec0ca6409010d";
        byte[] pmtSerialized = Hex.decode(pmtSerializedEncoded);
        Assert.assertThat(PartialMerkleTreeFormatUtils.getHashesCount(pmtSerialized).value, is(2L));
    }

    @Test
    public void getFlagBitsCount() {
        String pmtSerializedEncoded = "030000000279e7c0da739df8a00f12c0bff55e5438f530aa5859ff9874258cd7bad3fe709746aff89" +
                "7e6a851faa80120d6ae99db30883699ac0428fc7192d6c3fec0ca6409010d";
        byte[] pmtSerialized = Hex.decode(pmtSerializedEncoded);
        Assert.assertThat(PartialMerkleTreeFormatUtils.getFlagBitsCount(pmtSerialized).value, is(1L));
    }

    @Test
    public void hasExpectedSize() {
        String pmtSerializedEncoded = "030000000279e7c0da739df8a00f12c0bff55e5438f530aa5859ff9874258cd7bad3fe709746aff89" +
                "7e6a851faa80120d6ae99db30883699ac0428fc7192d6c3fec0ca6409010d";
        byte[] pmtSerialized = Hex.decode(pmtSerializedEncoded);
        Assert.assertThat(PartialMerkleTreeFormatUtils.hasExpectedSize(pmtSerialized), is(true));
    }

    @Test
    public void notHasExpectedSize() {
        String pmtSerializedEncoded = "030000000279e7c0da739df8a00f12c0bff55e5438f530aa5859ff9874258cd7bad3fe709746aff89" +
                "7e6a851faa80120d6ae99db30883699ac0428fc7192d6c3fec0ca64010d";
        byte[] pmtSerialized = Hex.decode(pmtSerializedEncoded);
        Assert.assertThat(PartialMerkleTreeFormatUtils.hasExpectedSize(pmtSerialized), is(false));
    }

    @Test(expected = ArithmeticException.class)
    public void overflowSize() {
        String pmtSerializedEncoded = "0300ffffff79e7c0da739df8a00f12c0bff55e5438f530aa5859ff9874258cd7bad3fe709746aff89" +
                "7e6a851faa80120d6ae99db30883699ac0428fc7192d6c3fec0ca6409010d";
        byte[] pmtSerialized = Hex.decode(pmtSerializedEncoded);
        PartialMerkleTreeFormatUtils.getFlagBitsCount(pmtSerialized);
    }

}