package tools;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.core.UscAddress;
import co.usc.peg.AddressBasedAuthorizer;
import co.usc.ulordj.core.Address;
import co.usc.ulordj.core.Coin;
import co.usc.ulordj.params.TestNet3Params;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;

public class WhilelistUTAddress {

    private static int tries = 3;

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
        tries = 3;
        return whitelistAddress(bridgeConstants, whitelistAuthorisedAddress.toString(), pwd, utAddress.toString(), BigInteger.valueOf(valueInSatoshi.value));
    }

    public static boolean whitelistAddress(BridgeConstants bridgeConstants, String whitelistAuthorisedAddress, String pwd, String utAddress, BigInteger valueInSatoshi) {
        tries = 3;
        try {
            AddressBasedAuthorizer lockWhitelistChangeAuthorizer = bridgeConstants.getLockWhitelistChangeAuthorizer();
            if (!lockWhitelistChangeAuthorizer.isAuthorized(new UscAddress(whitelistAuthorisedAddress)))
                return false;

            // Try to unlock account
            if(!Utils.tryUnlockUscAccount(whitelistAuthorisedAddress, pwd))
                return false;

            String encodedCmd = DataEncoder.encodeWhitelist(utAddress, valueInSatoshi);

            if(sendTransaction(whitelistAuthorisedAddress, encodedCmd))
                return true;

            return false;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }

    private static boolean sendTransaction(String whitelistAuthorisedAddress, String encodedCmd) throws IOException, InterruptedException {
        if(tries < 0)
            return false;
        tries--;
        String rpcCall = "{" +
                "\"jsonrpc\":\"2.0\", " +
                " \"method\":\"eth_sendTransaction\", " +
                " \"params\":[{" +
                " \"from\":\"" + whitelistAuthorisedAddress + "\"," +
                " \"to\":\"" + PrecompiledContracts.BRIDGE_ADDR_STR + "\"," +
                " \"gas\":\"0x3D0900\"," +
                " \"gasPrice\": \"0x9184e72a000\"," +
                " \"data\":\"" + encodedCmd + "\"}]," +
                " \"id\":\"1\"" +
                "}";
        System.out.println(rpcCall);

        StringEntity entity = new StringEntity(rpcCall, ContentType.APPLICATION_JSON);
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(NetworkConstants.POST_URI);
        httpPost.setEntity(entity);
        HttpResponse response = httpClient.execute(httpPost);

        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

        String txId = jsonObject.get("result").toString();
        System.out.println(txId);

        Thread.sleep(1000);
        if (!Utils.isTransactionInMemPool(txId))
            sendTransaction(whitelistAuthorisedAddress, encodedCmd);

        while (!Utils.isTransactionMined(txId)) {
            if(!Utils.isTransactionInMemPool(txId))
                sendTransaction(whitelistAuthorisedAddress, encodedCmd);
            Thread.sleep(1000 * 10);
        }
        return true;
    }
}
