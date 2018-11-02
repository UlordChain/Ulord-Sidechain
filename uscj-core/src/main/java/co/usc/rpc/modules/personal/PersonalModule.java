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

package co.usc.rpc.modules.personal;

import co.usc.config.UscSystemProperties;
import org.ethereum.rpc.Web3;

public interface PersonalModule {
    String dumpRawKey(String address) throws Exception;

    String importRawKey(String key, String passphrase);

    void init(UscSystemProperties properties);

    String[] listAccounts();

    boolean lockAccount(String address);

    String newAccountWithSeed(String seed);

    String newAccount(String passphrase);

    String sendTransaction(Web3.CallArguments args, String passphrase) throws Exception;

    boolean unlockAccount(String address, String passphrase, String duration);
}
