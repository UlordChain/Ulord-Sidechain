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

package co.usc.peg;

import co.usc.ulordj.core.*;
import co.usc.ulordj.wallet.RedeemData;
import co.usc.ulordj.wallet.Wallet;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author ajlopez
 * @author Oscar Guindzberg
 */
public class BridgeUldWallet extends Wallet {
    private List<Federation> federations;
    private Context uldContext;

    public BridgeUldWallet(Context uldContext, List<Federation> federations) {
        super(uldContext);
        this.federations = federations;
        this.uldContext = uldContext;
    }

    /*
     Method is overridden because implementation in parent is kind of buggy: does not check watched scripts
     */
    @Nullable
    @Override
    public RedeemData findRedeemDataFromScriptHash(byte[] payToScriptHash) {
        Context.propagate(this.uldContext);
        Optional<Federation> destinationFederation = federations.stream().filter(federation -> Arrays.equals(federation.getP2SHScript().getPubKeyHash(), payToScriptHash)).findFirst();
        if (!destinationFederation.isPresent()) {
            return null;
        }
        return RedeemData.of(destinationFederation.get().getPublicKeys(), destinationFederation.get().getRedeemScript());
    }
}
