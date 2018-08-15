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

package org.ethereum.validator;

import co.usc.core.UscAddress;
import co.usc.core.BlockDifficulty;
import org.ethereum.core.BlockHeader;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Angel J Lopez
 * @since 02.23.2016
 */
public class ParentNumberRuleTest {
    private ParentNumberRule rule = new ParentNumberRule();

    @Test // pass rule
    public void parentNumberEqualBlockNumberMinusOne() {
        BlockHeader header = getHeader(10000);
        BlockHeader parent = getHeader(9999);
        assertTrue(rule.validate(header, parent));
    }

    @Test // no pass rule
    public void parentNumberEqualBlockNumber() {
        BlockHeader header = getHeader(100);
        BlockHeader parent = getHeader(100);
        assertFalse(rule.validate(header, parent));
    }

    @Test // no pass rule
    public void parentNumberGreaterThanBlockNumber() {
        BlockHeader header = getHeader(100);
        BlockHeader parent = getHeader(101);
        assertFalse(rule.validate(header, parent));
    }

    private static BlockHeader getHeader(long number) {
        BlockHeader header = new BlockHeader(null, null, UscAddress.nullAddress().getBytes(),
                null, BlockDifficulty.ZERO.getBytes(), number, null, 0,
                0, null, null, 0);

        return header;
    }
}