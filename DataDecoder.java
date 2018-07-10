package tools;

import co.usc.crypto.Keccak256;
import co.usc.peg.Bridge;
import co.usc.peg.BridgeSerializationUtils;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.core.UldTransaction;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.util.SortedMap;

public class DataDecoder {

    private static String getResult(String response) {
        try {
            // Check if result is in JSON format
            JSONObject jsonObject = new JSONObject(response);
            return jsonObject.getString("result").substring(2);
        }
        catch (Exception e) {
            if (response.startsWith("0x"))
                response = response.substring(2);
            return response;
        }
    }

    public static String[] decodeGetFederationAddress(String response) {
        Object[] objects = Bridge.GET_FEDERATION_ADDRESS.decodeResult(Hex.decode(getResult(response)));

        String[] addresses = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            addresses[i] = objects[i].toString();
        }
        return addresses;
    }

    public static SortedMap<Keccak256, UldTransaction> decodeGetStateForUlordReleaseClient(String response, NetworkParameters params) {
        Object[] objects = Bridge.GET_STATE_FOR_ULD_RELEASE_CLIENT.decodeResult(Hex.decode(getResult(response)));
        byte[] data = (byte[])objects[0];
        RLPList rlpList = (RLPList) RLP.decode2(data).get(0);

        return BridgeSerializationUtils.deserializeMap(rlpList.get(0).getRLPData(), params, false);
    }

    public static String decodeGetPendingFederationHash(String response) {
        return Hex.toHexString((byte[])Bridge.GET_PENDING_FEDERATION_HASH.decodeResult(Hex.decode(getResult(response)))[0]);
    }

    public static Integer decodeGetPendingFederationSize(String response) {
        Object[] objects = Bridge.GET_PENDING_FEDERATION_SIZE.decodeResult(Hex.decode(getResult(response)));
        return Integer.valueOf(objects[0].toString());
    }

    public static Integer decodeCreateFederation(String response) {
        Object[] objects = Bridge.CREATE_FEDERATION.decodeResult(Hex.decode(getResult(response)));
        return Integer.valueOf(objects[0].toString());
    }

    public static Integer decodeAddFederatorPublicKey(String response) {
        Object[] objects = Bridge.ADD_FEDERATOR_PUBLIC_KEY.decodeResult(Hex.decode(getResult(response)));
        return Integer.valueOf(objects[0].toString());
    }

    public static String decodeGetPendingFederatorPublicKey(String response) {
        Object[] objects = Bridge.ADD_FEDERATOR_PUBLIC_KEY.decodeResult(Hex.decode(getResult(response)));
        return Hex.toHexString((byte[])objects[0]);
    }

    public static Integer decodeGetFederationThreshold(String response) {
        Object[] objects = Bridge.GET_FEDERATION_THRESHOLD.decodeResult(Hex.decode(getResult(response)));
        return Integer.valueOf(objects[0].toString());
    }

    public static String decodeGetFederatorPublicKey(String response) {
        return Hex.toHexString((byte[]) Bridge.GET_FEDERATOR_PUBLIC_KEY.decodeResult(Hex.decode(getResult(response)))[0]);
    }

    public static String decodeGetRetiringFederationAddress(String response) {
        Object[] objects = Bridge.GET_RETIRING_FEDERATION_ADDRESS.decodeResult(Hex.decode(getResult(response)));
        return objects[0].toString();
    }
}
