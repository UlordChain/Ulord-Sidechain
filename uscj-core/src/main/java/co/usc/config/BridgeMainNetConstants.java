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

        UldECKey federator0PublicKey = UldECKey.fromPublicOnly(Hex.decode("0227f5a699b3298a5d48af12d883a7cd6733887e80532fd114b9c623de7d1d37ec"));
//        UldECKey federator1PublicKey = UldECKey.fromPublicOnly(Hex.decode("027319afb15481dbeb3c426bcc37f9a30e7f51ceff586936d85548d9395bcc2344"));
//        UldECKey federator2PublicKey = UldECKey.fromPublicOnly(Hex.decode("0355a2e9bf100c00fc0a214afd1bf272647c7824eb9cb055480962f0c382596a70"));
//        UldECKey federator3PublicKey = UldECKey.fromPublicOnly(Hex.decode("02566d5ded7c7db1aa7ee4ef6f76989fb42527fcfdcddcd447d6793b7d869e46f7"));
//        UldECKey federator4PublicKey = UldECKey.fromPublicOnly(Hex.decode("0294c817150f78607566e961b3c71df53a22022a80acbb982f83c0c8baac040adc"));
//        UldECKey federator5PublicKey = UldECKey.fromPublicOnly(Hex.decode("0372cd46831f3b6afd4c044d160b7667e8ebf659d6cb51a825a3104df6ee0638c6"));
//        UldECKey federator6PublicKey = UldECKey.fromPublicOnly(Hex.decode("0340df69f28d69eef60845da7d81ff60a9060d4da35c767f017b0dd4e20448fb44"));
//        UldECKey federator7PublicKey = UldECKey.fromPublicOnly(Hex.decode("02ac1901b6fba2c1dbd47d894d2bd76c8ba1d296d65f6ab47f1c6b22afb53e73eb"));
//        UldECKey federator8PublicKey = UldECKey.fromPublicOnly(Hex.decode("031aabbeb9b27258f98c2bf21f36677ae7bae09eb2d8c958ef41a20a6e88626d26"));
//        UldECKey federator9PublicKey = UldECKey.fromPublicOnly(Hex.decode("0245ef34f5ee218005c9c21227133e8568a4f3f11aeab919c66ff7b816ae1ffeea"));
//        UldECKey federator10PublicKey = UldECKey.fromPublicOnly(Hex.decode("02550cc87fa9061162b1dd395a16662529c9d8094c0feca17905a3244713d65fe8"));
//        UldECKey federator11PublicKey = UldECKey.fromPublicOnly(Hex.decode("02481f02b7140acbf3fcdd9f72cf9a7d9484d8125e6df7c9451cfa55ba3b077265"));
//        UldECKey federator12PublicKey = UldECKey.fromPublicOnly(Hex.decode("03f909ae15558c70cc751aff9b1f495199c325b13a9e5b934fd6299cd30ec50be8"));
//        UldECKey federator13PublicKey = UldECKey.fromPublicOnly(Hex.decode("02c6018fcbd3e89f3cf9c7f48b3232ea3638eb8bf217e59ee290f5f0cfb2fb9259"));
//        UldECKey federator14PublicKey = UldECKey.fromPublicOnly(Hex.decode("03b65694ccccda83cbb1e56b31308acd08e993114c33f66a456b627c2c1c68bed6"));

        List<UldECKey> genesisFederationPublicKeys = Lists.newArrayList(
                federator0PublicKey /*, federator1PublicKey, federator2PublicKey,
                federator3PublicKey, federator4PublicKey, federator5PublicKey,
                federator6PublicKey, federator7PublicKey, federator8PublicKey,
                federator9PublicKey, federator10PublicKey, federator11PublicKey,
                federator12PublicKey, federator13PublicKey, federator14PublicKey */
        );

        // Currently set to:
        // Wednesday, September 5, 2018 9:00:00 AM GMT+08:00
        Instant genesisFederationAddressCreatedAt = Instant.ofEpochMilli(1536116400l);
        
        genesisFederation = new Federation(
                genesisFederationPublicKeys,
                genesisFederationAddressCreatedAt,
                1L,
                getUldParams()
        );

        uld2UscMinimumAcceptableConfirmations = 100;
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
