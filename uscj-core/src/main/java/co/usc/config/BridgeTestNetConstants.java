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

import co.usc.ulordj.core.UldECKey;
import co.usc.ulordj.core.Coin;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.peg.AddressBasedAuthorizer;
import co.usc.peg.Federation;
import com.google.common.collect.Lists;
import org.ethereum.crypto.ECKey;
import org.bouncycastle.util.encoders.Hex;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BridgeTestNetConstants extends BridgeConstants {
    private static BridgeTestNetConstants instance = new BridgeTestNetConstants();

    BridgeTestNetConstants() {
        uldParamsString = NetworkParameters.ID_TESTNET;

        UldECKey federator0PublicKey = UldECKey.fromPublicOnly(Hex.decode("0239198147715180cca673ce61fdfee88f06515d2bb16c74d2e5c726c688dacbc0"));
        UldECKey federator1PublicKey = UldECKey.fromPublicOnly(Hex.decode("02be2cb017862c83e989f26afaec79ba2b1d2c417a894f56e13f7a301797ab7b35"));
        UldECKey federator2PublicKey = UldECKey.fromPublicOnly(Hex.decode("028fc93d435103f8caaf3bf59682d6fddc037cedb67e4f78efd9bc9a0538918cfc"));
        UldECKey federator3PublicKey = UldECKey.fromPublicOnly(Hex.decode("03c281ca3cab2d273c5ee1ca50dd62f941d1bc04dc2ca79affb2606660f8fd54ea"));

        List<UldECKey> genesisFederationPublicKeys = Lists.newArrayList(
                federator0PublicKey,
                federator1PublicKey,
                federator2PublicKey,
                federator3PublicKey
        );

        // Currently set to:
        // Wednesday, January 3, 2018 12:00:00 AM GMT-03:00
        Instant genesisFederationAddressCreatedAt = Instant.ofEpochMilli(1531958400);

        genesisFederation = new Federation(
                genesisFederationPublicKeys,
                genesisFederationAddressCreatedAt,
                1L,
                getUldParams()
        );

        uld2UscMinimumAcceptableConfirmations = 10;
        uld2UscMinimumAcceptableConfirmationsOnUsc = 10;
        usc2UldMinimumAcceptableConfirmations = 10;

        updateBridgeExecutionPeriod = 3 * 60 * 1000; // 3 minutes

        maxUldHeadersPerUscBlock = 100;

        minimumLockTxValue = Coin.valueOf(1000000);
        minimumReleaseTxValue = Coin.valueOf(500000);

        // Passphrases are kept private
        List<ECKey> federationChangeAuthorizedKeys = Arrays.stream(new String[]{
            "0465018e290308764ba2463928a25e0951c942df584cfdd951b195d2b73e8bdf43b52d4a1673e6ac84d0ffe12c9d91e8ccfdcaf7d3e743b221ab34704c903cc01f",
            "041035e6579118189d447ffc02d527a5faee59463116cd04401822809b604249ac7729b3744523034a9956edf20647b8fa6ed4cf437e9f9f0c40ecff70bb9db0af",
            "04e13d7a740942213965943409ec8125f83fad1b539b77724cb2853cba7d0596d618de9adf5a995a9d7b0c6b7729abfb0b25a35f3e47b7d02332ae6bfbe24504dc"
        }).map(hex -> ECKey.fromPublicOnly(Hex.decode(hex))).collect(Collectors.toList());

        federationChangeAuthorizer = new AddressBasedAuthorizer(
                federationChangeAuthorizedKeys,
                AddressBasedAuthorizer.MinimumRequiredCalculation.MAJORITY
        );

        // Passphrases are kept private
        List<ECKey> lockWhitelistAuthorizedKeys = Arrays.stream(new String[]{
            "04d461bd5518f0720bfdcb41a520a30a6f6cba03d26e387394cdc6cd2d2859c0731661d673a296aaaa8437d6828c44a165a4ed9b4f10ab17ba429a0c5677971874"
        }).map(hex -> ECKey.fromPublicOnly(Hex.decode(hex))).collect(Collectors.toList());

        lockWhitelistChangeAuthorizer = new AddressBasedAuthorizer(
                lockWhitelistAuthorizedKeys,
                AddressBasedAuthorizer.MinimumRequiredCalculation.ONE
        );

        federationActivationAge = 60L;

        fundsMigrationAgeSinceActivationBegin = 60L;
        fundsMigrationAgeSinceActivationEnd = 900L;

        List<ECKey> feePerKbAuthorizedKeys = Arrays.stream(new String[]{
            "04d42e09ff5c4188545ce6e87fc4f014c60d59f3bea125a9854509de8a4df4a883451ceb91bce47834666030f90593063f172338b980af39d2c29519715371ef42",
            "047664a07e5aa7f985bc72543cf532de25b28344abce12a39218128033b2615a32dd61ce03fee83c141950591c9b4507f936eec86d05cfbd67aafc221465f83e64",
            "04e943913af64bf4e993c58a6b346fcc69c13c058bd7c147b2276048ba83f3ed1462b04aaf5a44808c9e04392a1b20e5bdc43bcb0bae481c74d454c4fce59b4558"
        }).map(hex -> ECKey.fromPublicOnly(Hex.decode(hex))).collect(Collectors.toList());

        feePerKbChangeAuthorizer = new AddressBasedAuthorizer(
                feePerKbAuthorizedKeys,
                AddressBasedAuthorizer.MinimumRequiredCalculation.MAJORITY
        );

        genesisFeePerKb = Coin.MICROCOIN;
    }

    public static BridgeTestNetConstants getInstance() {
        return instance;
    }

}
