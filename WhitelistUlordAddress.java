package tools;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.core.UscAddress;
import co.usc.peg.AddressBasedAuthorizer;
import co.usc.ulordj.core.Address;
import co.usc.ulordj.core.Coin;
import co.usc.ulordj.params.TestNet3Params;
import org.ethereum.vm.PrecompiledContracts;

import java.math.BigInteger;

public class WhitelistUlordAddress {

    public static void main(String[] args) {
        BridgeConstants bridgeConstants = BridgeTestNetConstants.getInstance();
        Address utAddress = new Address(TestNet3Params.get(), "ubhhcfxeZ2VJRADHEx5uqUBJ1HTip8cfRS");
        UscAddress whitelistAuthrisedAddress = new UscAddress("ddc165ce2c9026909856387fe50ff1e13ec22eb9");
        String pwd = "abcd1234";
        if(whitelistAddress(bridgeConstants, whitelistAuthrisedAddress, pwd, utAddress, Coin.valueOf(100_000_000_000l))) {
            System.out.println("Successfully whitelisted");
        }
        else
            System.out.println("Whitelist failed");
    }

    public static boolean whitelistAddress(BridgeConstants bridgeConstants, UscAddress whitelistAuthorisedAddress, String pwd, Address utAddress, Coin valueInSatoshi) {
        return whitelistAddress(bridgeConstants, whitelistAuthorisedAddress.toString(), pwd, utAddress.toString(), BigInteger.valueOf(valueInSatoshi.value));
    }

    public static boolean whitelistAddress(BridgeConstants bridgeConstants, String whitelistAuthorisedAddress, String pwd, String utAddress, BigInteger valueInSatoshi) {
        try {
            AddressBasedAuthorizer lockWhitelistChangeAuthorizer = bridgeConstants.getLockWhitelistChangeAuthorizer();
            if (!lockWhitelistChangeAuthorizer.isAuthorized(new UscAddress(whitelistAuthorisedAddress)))
                return false;

            // Try to unlock account
            if(!Utils.tryUnlockUscAccount(whitelistAuthorisedAddress, pwd))
                throw new PrivateKeyNotFoundException();

            String encodedCmd = DataEncoder.encodeWhitelist(utAddress, valueInSatoshi);

            if(Utils.sendTransaction(whitelistAuthorisedAddress, PrecompiledContracts.BRIDGE_ADDR_STR, "0x3D0900", "0x9184e72a000", null, encodedCmd, null, 3))
                return true;

            return false;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }
}
