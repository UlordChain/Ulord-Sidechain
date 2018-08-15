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

/**
 * Created by mario on 09/09/2016.
 */
public class CacheElement<T> {

    private T value;
    private Long lastAccess;
    private Long timeToLive;

    public CacheElement(T value, Long timeToLive) {
        this.value = value;
        this.lastAccess = System.currentTimeMillis();
        this.timeToLive = timeToLive;
    }

    public T value() {
        return value;
    }

    public void updateLastAccess() {
        this.lastAccess = System.currentTimeMillis();
    }

    public Boolean hasExpired() {
        Long now = System.currentTimeMillis();
        return (now - this.lastAccess) >= this.timeToLive;
    }
}
