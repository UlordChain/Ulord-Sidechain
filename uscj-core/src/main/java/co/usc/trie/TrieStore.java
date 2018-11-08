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

package co.usc.trie;

/**
 * Created by ajlopez on 29/03/2017.
 */
public interface TrieStore {
    void save(Trie trie);

    int getSaveCount();

    Trie retrieve(byte[] hash);

    int getRetrieveCount();

    byte[] serialize();

    byte[] retrieveValue(byte[] hash);

    interface Factory {
        TrieStore newInstance(String name);
    }
}
