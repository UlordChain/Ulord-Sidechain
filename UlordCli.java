package tools;

import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.RegTestParams;
import co.usc.ulordj.params.TestNet3Params;

import javax.annotation.Nullable;
import java.io.IOException;

public class UlordCli {

    private static String getNetworkCommand(NetworkParameters params) {
        if(params instanceof TestNet3Params)
            return "ulord-cli -testnet";
        else if(params instanceof RegTestParams)
            return "ulord-cli -regtest";
        return "ulord-cli ";
    }

    public static String getBlock(NetworkParameters params, String hash, boolean jsonFormat) throws IOException {
        String res = UlordCliExecutor.execute(getNetworkCommand(params) + " getblock " + hash + " "  + String.valueOf(jsonFormat));
        return res;
    }

    public static String getBlockHash(NetworkParameters params, int height) throws IOException {
        String res = UlordCliExecutor.execute(getNetworkCommand(params) + " getblockhash "+ height);
        return res;
    }

    public static String getBlockHeader(NetworkParameters params, String hash) throws IOException {
        return getBlockHeader(params, hash, true);
    }

    public static String getBlockHeader(NetworkParameters params, String hash, boolean verbose) throws IOException {
        String res = UlordCliExecutor.execute(getNetworkCommand(params) + " getblockheader "+ hash +" "+ verbose);
        return res;
    }

    public static String getRawTransaction(NetworkParameters params, String txId, boolean jsonFormat) throws IOException {
        int val = jsonFormat == true ? 1 : 0;
        String result = UlordCliExecutor.execute(getNetworkCommand(params) + " getrawtransaction " + txId + " " + val);
        return result;
    }

    public  static String getBlockCount(NetworkParameters params) throws IOException {
        return UlordCliExecutor.execute(getNetworkCommand(params) + " getblockcount");
    }

    public static String sendRawTransaction(NetworkParameters params, String txHex) throws  IOException {
        return UlordCliExecutor.execute(getNetworkCommand(params) + " sendrawtransaction " + txHex);
    }

    public static String decodeRawTransaction(NetworkParameters params, String rawTxHex) throws IOException {
        return UlordCliExecutor.execute(getNetworkCommand(params) + " decoderawtransaction " + rawTxHex);
    }

    public static String getAddressUtxos(NetworkParameters params, String[] addresses) throws IOException {
        String adds = "";
        for (int i = 0; i < addresses.length; ++i) {
            if(i == addresses.length - 1)
               adds += "\"" + addresses[i] + "\"";
            else
                adds +=  "\"" + addresses[i] + "\",";
        }
        return UlordCliExecutor.execute(getNetworkCommand(params) + " getaddressutxos '{\"addresses\":[" + adds + "]}'");
    }

    public static String dumpPrivKey(NetworkParameters params,  String address) throws  IOException {
        return UlordCliExecutor.execute(getNetworkCommand(params) + " dumpprivkey " + address);
    }

    //signrawtransaction "hexstring" ( [{"txid":"id","vout":n,"scriptPubKey":"hex","redeemScript":"hex"},...] ["privatekey1",...] sighashtype )
    public static String signRawTransaction(NetworkParameters params,
                                            String rawTx,
                                            String txId,
                                            int vout,
                                            String scriptPubKey,
                                            String redeemScript,
                                            String[] privKeys,
                                            @Nullable String sigHashType) throws IOException {
        String cmd = getNetworkCommand(params) + " signrawtransaction" +
                " '" + rawTx + "'" +
                        " '[{" +
                        " \"txid\":"         + "\"" + txId + "\"," +
                        " \"vout\":"         + vout + ","+
                        " \"scriptPubKey\":" + "\"" + scriptPubKey + "\"," +
                        " \"redeemScript\":" + "\"" + redeemScript + "\"" +
                        "}]'"  +
                        " '[";
        for(int i = 0; i < privKeys.length; ++i) {
            if(i == privKeys.length - 1)
                cmd += "\"" + privKeys[i] +"\"";
            else
                cmd += "\"" + privKeys[i] +"\", ";
        }
        cmd += "]'";
        if(sigHashType != null)
            cmd += " " + sigHashType;

        return UlordCliExecutor.execute(cmd);
    }
}
