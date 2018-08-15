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

package co.usc.config;

import co.usc.ulordj.core.Coin;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.peg.AddressBasedAuthorizer;
import co.usc.peg.Federation;

public class BridgeConstants {
    protected String uldParamsString;

    protected Federation genesisFederation;

    protected int uld2UscMinimumAcceptableConfirmations;
    protected int uld2UscMinimumAcceptableConfirmationsOnUsc;
    protected int usc2UldMinimumAcceptableConfirmations;
    protected int uldBroadcastingMinimumAcceptableBlocks;

    protected int updateBridgeExecutionPeriod;

    protected int maxUldHeadersPerUscBlock;

    protected Coin minimumLockTxValue;
    protected Coin minimumReleaseTxValue;

    protected long federationActivationAge;

    protected long fundsMigrationAgeSinceActivationBegin;
    protected long fundsMigrationAgeSinceActivationEnd;

    protected AddressBasedAuthorizer federationChangeAuthorizer;

    protected AddressBasedAuthorizer lockWhitelistChangeAuthorizer;

    protected AddressBasedAuthorizer feePerKbChangeAuthorizer;

    protected Coin genesisFeePerKb;

    public NetworkParameters getUldParams() {
        return NetworkParameters.fromID(uldParamsString);
    }

    public String getUldParamsString() {
        return uldParamsString;
    }

    public Federation getGenesisFederation() { return genesisFederation; }

    public int getUld2UscMinimumAcceptableConfirmations() {
        return uld2UscMinimumAcceptableConfirmations;
    }

    public int getUld2UscMinimumAcceptableConfirmationsOnUsc() {
        return uld2UscMinimumAcceptableConfirmationsOnUsc;
    }

    public int getUsc2UldMinimumAcceptableConfirmations() {
        return usc2UldMinimumAcceptableConfirmations;
    }

    public int getUpdateBridgeExecutionPeriod() { return updateBridgeExecutionPeriod; }

    public int getMaxUldHeadersPerUscBlock() { return maxUldHeadersPerUscBlock; }

    public Coin getMinimumLockTxValue() { return minimumLockTxValue; }

    public Coin getMinimumReleaseTxValue() { return minimumReleaseTxValue; }

    public long getFederationActivationAge() { return federationActivationAge; }

    public long getFundsMigrationAgeSinceActivationBegin() {
        return fundsMigrationAgeSinceActivationBegin;
    }

    public long getFundsMigrationAgeSinceActivationEnd() {
        return fundsMigrationAgeSinceActivationEnd;
    }

    public AddressBasedAuthorizer getFederationChangeAuthorizer() { return federationChangeAuthorizer; }

    public AddressBasedAuthorizer getLockWhitelistChangeAuthorizer() { return lockWhitelistChangeAuthorizer; }

    public AddressBasedAuthorizer getFeePerKbChangeAuthorizer() { return feePerKbChangeAuthorizer; }

    public Coin getGenesisFeePerKb() { return genesisFeePerKb; }
}
