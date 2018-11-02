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
package co.usc.peg.whitelist;

import co.usc.ulordj.core.Address;
import co.usc.ulordj.core.Coin;

public class UnlimitedWhiteListEntry implements LockWhitelistEntry {
    private final Address address;

    public UnlimitedWhiteListEntry(Address address) {
        this.address = address;
    }

    public Address address() {
        return this.address;
    }

    public void consume() {
        // Unlimited whitelisting means that the entries are never fully consumed so nothing to do here
    }

    public boolean isConsumed() {
        return false;
    }

    public boolean canLock(Coin value) {
        return true;
    }
}
