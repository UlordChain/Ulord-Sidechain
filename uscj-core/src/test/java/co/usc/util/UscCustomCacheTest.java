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

package co.usc.util;

import org.ethereum.core.BlockHeader;
import org.ethereum.db.ByteArrayWrapper;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Created by mario on 09/09/2016.
 */
public class UscCustomCacheTest {

    private static Long TIME_TO_LIVE = 2000L;
    private static Long WAIT_PERIOD = 1000L;

    private static ByteArrayWrapper KEY = new ByteArrayWrapper(new byte[]{12, 12, 12, 121, 12});
    private static ByteArrayWrapper OTHER_KEY = new ByteArrayWrapper(new byte[]{11, 11, 11, 111, 11});


    @Test
    public void createBlockHeaderCache() {
        Assert.assertNotNull(new UscCustomCache(TIME_TO_LIVE));
    }

    @Test
    public void addElement() {
        UscCustomCache cache = new UscCustomCache(TIME_TO_LIVE);

        BlockHeader header1 = Mockito.mock(BlockHeader.class);
        cache.put(KEY, header1);

        Assert.assertNotNull(cache.get(KEY));
        Assert.assertEquals(header1, cache.get(KEY));
    }

    @Test
    public void getElement() {
        UscCustomCache cache = new UscCustomCache(TIME_TO_LIVE);

        BlockHeader header1 = Mockito.mock(BlockHeader.class);
        cache.put(KEY, header1);

        Assert.assertNotNull(cache.get(KEY));
        Assert.assertNull(cache.get(OTHER_KEY));
    }

    @Test
    @Ignore
    public void elementExpiration() throws InterruptedException{
        UscCustomCache cache = new UscCustomCache(800L);

        BlockHeader header1 = Mockito.mock(BlockHeader.class);
        cache.put(KEY, header1);
        BlockHeader header2 = Mockito.mock(BlockHeader.class);
        cache.put(OTHER_KEY, header2);

        Assert.assertEquals(header1, cache.get(KEY));
        Assert.assertEquals(header2, cache.get(OTHER_KEY));
        cache.get(OTHER_KEY);
        Thread.sleep(700);
        Assert.assertEquals(header2, cache.get(OTHER_KEY));
        Thread.sleep(400);

        //header2 should not be removed, it was accessed
        Assert.assertNotNull(cache.get(OTHER_KEY));
        Assert.assertNull(cache.get(KEY));

        Thread.sleep(2*WAIT_PERIOD);
        //header2 should be removed, it was not accessed
        Assert.assertNull(cache.get(OTHER_KEY));
    }




}
