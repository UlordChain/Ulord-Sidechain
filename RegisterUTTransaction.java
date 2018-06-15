/*
 * This file is part of Usc
 * Copyright (C) 2016 - 2018 Ulord development team.
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

package tools;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.core.UscAddress;
import co.usc.peg.AddressBasedAuthorizer;
import co.usc.ulordj.core.UldTransaction;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.TestNet3Params;

public class RegisterUTTransaction {

    public static void main(String[]args){
        //registerUldTransaction <tx hex> <height> <merkletree>
        registerUT(BridgeTestNetConstants.getInstance(), "674f05e1916abc32a38f40aa67ae6b503b565999", "abcd1234", "fe472c8a0ad06f7ba682fe5a60bf9245496928be8f1e18a47ce6d4682921a176");
    }

    public static boolean registerUT(BridgeConstants bridgeConstants, String fedAddress, String pwd, String utTxId) {
        try {
            AddressBasedAuthorizer federationChangeAuthorizer = bridgeConstants.getFederationChangeAuthorizer();

            if (!federationChangeAuthorizer.isAuthorized(new UscAddress(fedAddress)))
                return false;

            // Try to unlock account
            if (!Utils.tryUnlockUscAccount(fedAddress, pwd))
                throw new PrivateKeyNotFoundException();

            UldTransaction tx;
            if(bridgeConstants instanceof BridgeTestNetConstants)
                tx = Utils.getUldTransaction(TestNet3Params.get(), utTxId);
            else
                tx = Utils.getUldTransaction(MainNetParams.get(), utTxId);


            return true;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }
}
