package tools;

import co.usc.crypto.Keccak256;
import co.usc.peg.Bridge;
import co.usc.ulordj.core.UldTransaction;
import org.spongycastle.util.encoders.Hex;

import java.util.SortedMap;

public class DataDecoder {

    private static String getResult(String result) {
        if(result.startsWith("0x"))
            result = result.substring(2);
        return result;
    }

    public static String[] decodeGetFederationAddress(String result) {
        Object[] objects = Bridge.GET_FEDERATION_ADDRESS.decodeResult(Hex.decode(getResult(result)));

        String[] addresses = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            addresses[i] = objects[i].toString();
        }
        return addresses;
    }

//    public static SortedMap<Keccak256, UldTransaction> decodeGetStateForUlordReleaseClient(String result) {
//        Object[] objects = Bridge.GET_STATE_FOR_ULD_RELEASE_CLIENT.decodeResult(Hex.decode(getResult(result)));
//
//    }
}
