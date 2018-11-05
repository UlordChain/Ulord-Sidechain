package co.usc.config;

import co.usc.ulordj.core.UldECKey;
import co.usc.ulordj.core.Coin;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.peg.AddressBasedAuthorizer;
import co.usc.peg.Federation;
import co.usc.ulordj.params.MainNetParams;
import com.google.common.collect.Lists;
import org.ethereum.crypto.ECKey;
import org.bouncycastle.util.encoders.Hex;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BridgeMainNetConstants extends BridgeConstants {
    private static BridgeMainNetConstants instance = new BridgeMainNetConstants();

    BridgeMainNetConstants() {
        uldParamsString = NetworkParameters.ID_MAINNET;

        UldECKey federator0PublicKey = UldECKey.fromPublicOnly(Hex.decode("020348c97538ac9e16b4cd2deeefe89697a668afed1ce3b50623a9b8a723e0e701"));
        UldECKey federator1PublicKey = UldECKey.fromPublicOnly(Hex.decode("026491631d7515ca29bf2efe6cf690ecdc1d1df4b6c6080a53eb9e4dad7499ffda"));
        UldECKey federator2PublicKey = UldECKey.fromPublicOnly(Hex.decode("02d17083493dcd1f769671e17cf7d9eae00ebf0f550c6c05f033c155ba42c028b9"));
        UldECKey federator3PublicKey = UldECKey.fromPublicOnly(Hex.decode("024f6b270ecf55453eade152e7799e48776b036aa8a34f0593c921e0a961882602"));
        UldECKey federator4PublicKey = UldECKey.fromPublicOnly(Hex.decode("0200fa56840ed8cfe3ef6a50aebea29576bf1230e7b2d73a45d68c812879afb4a1"));

        List<UldECKey> genesisFederationPublicKeys = Lists.newArrayList(
                federator0PublicKey , federator1PublicKey, federator2PublicKey,
                federator3PublicKey, federator4PublicKey
        );

        // Currently set to:
        // Tuesday, September 28, 2018 9:00:00 AM GMT+08:00
        Instant genesisFederationAddressCreatedAt = Instant.ofEpochMilli(1538096400L);
        
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
                "043257bcb2d10be56236b30ce6132ca5e1e610f283fb0dbf8d5c0f6ca9b386ecb5f2f54194ff3a5faab19f2bc159783d37625fc93fe9b655e8f112478e44b624b5"
        }).map(hex -> ECKey.fromPublicOnly(Hex.decode(hex))).collect(Collectors.toList());

        federationChangeAuthorizer = new AddressBasedAuthorizer(
                federationChangeAuthorizedKeys,
                AddressBasedAuthorizer.MinimumRequiredCalculation.MAJORITY
        );

        List<ECKey> lockWhitelistAuthorizedKeys = Arrays.stream(new String[]{
                "04bfc718c430b76c86fe6e2ac080475c3da328d5ac2bc3cd57947d6ebe4bb546ec4e394d6bd069b61399a76804753ff4951482700cff93d5e0b338cfb163959634"
        }).map(hex -> ECKey.fromPublicOnly(Hex.decode(hex))).collect(Collectors.toList());

        lockWhitelistChangeAuthorizer = new AddressBasedAuthorizer(
                lockWhitelistAuthorizedKeys,
                AddressBasedAuthorizer.MinimumRequiredCalculation.ONE
        );

        federationActivationAge = 18500L;

        fundsMigrationAgeSinceActivationBegin = 0L;
        fundsMigrationAgeSinceActivationEnd = 10585L;

        List<ECKey> feePerKbAuthorizedKeys = Arrays.stream(new String[]{
                "0404f7821f390babd5403f3c56099404bb541785844da0ece6f394901507b4eb0a446a32ff51ad67f24a8053dc1f2b0bd9ed185b44a12c415f7f4ca4fdc826af0a"
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
