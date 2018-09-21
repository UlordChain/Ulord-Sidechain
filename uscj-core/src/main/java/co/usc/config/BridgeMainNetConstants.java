package co.usc.config;

import co.usc.ulordj.core.UldECKey;
import co.usc.ulordj.core.Coin;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.peg.AddressBasedAuthorizer;
import co.usc.peg.Federation;
import co.usc.ulordj.params.MainNetParams;
import com.google.common.collect.Lists;
import org.ethereum.crypto.ECKey;
import org.spongycastle.util.encoders.Hex;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BridgeMainNetConstants extends BridgeConstants {
    private static BridgeMainNetConstants instance = new BridgeMainNetConstants();

    BridgeMainNetConstants() {
        uldParamsString = NetworkParameters.ID_MAINNET;

        UldECKey federator0PublicKey = UldECKey.fromPublicOnly(Hex.decode("02a8a2d41c40435fd16041e41d3512c5f6c73984a78c4460f4d97ee25141589192"));
        UldECKey federator1PublicKey = UldECKey.fromPublicOnly(Hex.decode("03830fca9518200abfdaa2d81d411805f512688c654127e45f5739c33d21019d76"));
        UldECKey federator2PublicKey = UldECKey.fromPublicOnly(Hex.decode("02b8fe2d242f6c7aaa9c938331445acc3795c997a003cd84bba4d18b59ca0384b0"));
        UldECKey federator3PublicKey = UldECKey.fromPublicOnly(Hex.decode("0267ccf2dde9041aabdc65ccd50c3b32ae8587141e0eb17828bc07b9b4991afc2c"));
        UldECKey federator4PublicKey = UldECKey.fromPublicOnly(Hex.decode("03d95d034084acca7563a0f4d83a8287b4f9d361caa0d5704f058c7363601c3a65"));
        UldECKey federator5PublicKey = UldECKey.fromPublicOnly(Hex.decode("03d419e66c2f4ff4b8f68a1e2f91122e79c169aa049eea908fe28ba5aa4a748734"));

        List<UldECKey> genesisFederationPublicKeys = Lists.newArrayList(
                federator0PublicKey , federator1PublicKey, federator2PublicKey,
                federator3PublicKey, federator4PublicKey, federator5PublicKey
        );

        // Currently set to:
        // Tuesday, September 25, 2018 9:00:00 AM GMT+08:00
        Instant genesisFederationAddressCreatedAt = Instant.ofEpochMilli(1537837200L);
        
        genesisFederation = new Federation(
                genesisFederationPublicKeys,
                genesisFederationAddressCreatedAt,
                1L,
                getUldParams()
        );

        uld2UscMinimumAcceptableConfirmations = 20;
        uld2UscMinimumAcceptableConfirmationsOnUsc = 1000;
        usc2UldMinimumAcceptableConfirmations = 1000;

        updateBridgeExecutionPeriod = 3 * 60 * 1000; // 3 minutes

        maxUldHeadersPerUscBlock = 100;

        minimumLockTxValue = Coin.valueOf(1000000);
        minimumReleaseTxValue = Coin.valueOf(800000);

        List<ECKey> federationChangeAuthorizedKeys = Arrays.stream(new String[]{
                "04dfbe4afc74963cc7d561d1edd5c237ce5b9b2fa28ec747f8ffe1d18f060c6527b1efdb25b6d5bbff7ea921741e66558765198cefa037087aa9641d02630753bb",
                "0428eb6d7995f75265b2f41ecb1c7bc6671eb22e8f15d856c81715f97b65068228e7cb3a2dd556b9d7117b09a4ce24d411afb50ce0de080f345fbd74984df19db1",
                "04ef552498dfae3e8123091fbe3d00041117bb697feb0b882e4aae3a1965b39569193d665e77ef703dcf61fea51a87be3a6f36adc417b3a2ea27f3e8b5d593ef99"
        }).map(hex -> ECKey.fromPublicOnly(Hex.decode(hex))).collect(Collectors.toList());

        federationChangeAuthorizer = new AddressBasedAuthorizer(
                federationChangeAuthorizedKeys,
                AddressBasedAuthorizer.MinimumRequiredCalculation.MAJORITY
        );

        List<ECKey> lockWhitelistAuthorizedKeys = Arrays.stream(new String[]{
                "0491ae7171182dc2b6568b8b072bbd1879dfff821465fd80e82b5d0ce02cfb371b2adb741674dedf32a57dd1003273419550e70330ffc7819578ef85fe1c2c58d0"
        }).map(hex -> ECKey.fromPublicOnly(Hex.decode(hex))).collect(Collectors.toList());

        lockWhitelistChangeAuthorizer = new AddressBasedAuthorizer(
                lockWhitelistAuthorizedKeys,
                AddressBasedAuthorizer.MinimumRequiredCalculation.ONE
        );

        federationActivationAge = 18500L;

        fundsMigrationAgeSinceActivationBegin = 0L;
        fundsMigrationAgeSinceActivationEnd = 10585L;

        List<ECKey> feePerKbAuthorizedKeys = Arrays.stream(new String[]{
                "04f457e334ec381960d4ed5f506b956c24d6bc9f6a3968076baf5154a5ab6959df36d0c8af778aae5b784eb8339e41d2ae9b2c8db5c2f59965be1b726320d806c3",
                "045ade5c44820f2671872ee6f0fd1846ff01ca0da2a9c106097459ce9b47abe0e8b80966ffe423df2116a03bd0aee6c7a079f705f7676f6f88f5cb1db33ec99b93",
                "04985aaaa7102adb1db40a360b28f54007214d406416a9d899326ca9b95fb708092d78bbbe42c6a6e4a4716faf36e79c44f9ca7783f2610509be1924657b704bfd"
        }).map(hex -> ECKey.fromPublicOnly(Hex.decode(hex))).collect(Collectors.toList());

        feePerKbChangeAuthorizer = new AddressBasedAuthorizer(
                feePerKbAuthorizedKeys,
                AddressBasedAuthorizer.MinimumRequiredCalculation.MAJORITY
        );

        genesisFeePerKb = Coin.MILLICOIN.multiply(5);
    }

    public static BridgeMainNetConstants getInstance() {
        return instance;
    }

}
