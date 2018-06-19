package tools;

import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;

public class UscRpc {
    public static String unlockAccount(String address, String pwd) throws IOException {
        String cmd = "{" +
                "\"jsonrpc\":\"2.0\", " +
                "\"method\":\"personal_unlockAccount\", " +
                "\"id\":\"1\", " +
                "\"params\":[" +
                "\"" + address + "\", " +
                "\"" + pwd + "\", \"\"" +
                "]}";

        return UscRpcExecutor.execute(cmd);
    }

    public static String getTransactionByHash(String txId) throws IOException {
        String cmd = "{" +
                "\"jsonrpc\":\"2.0\", " +
                "\"method\":\"eth_getTransactionByHash\", " +
                "\"id\":\"1\", " +
                "\"params\":[" +
                "\"" + txId + "\"" +
                "]}";
        System.out.println(cmd);
        return UscRpcExecutor.execute(cmd);
    }

    public static boolean sendTransaction(String from,
                                          @Nullable String to,
                                          @Nullable String gas,
                                          @Nullable String gasPrice,
                                          @Nullable String value,
                                          @Nullable String data,
                                          @Nullable String nonce,
                                          int tries)
            throws IOException, InterruptedException {

        if(tries < 0)
            return false;

        StringBuilder cmd = new StringBuilder();
        cmd.append("{");
        cmd.append("\"jsonrpc\":\"2.0\", ");

        if(to != null)
            cmd.append("\"id\":\"1\", ");

        cmd.append("\"method\":\"eth_sendTransaction\", ");
        cmd.append("\"params\":[{");
        cmd.append("\"from\":\"" + from + "\", ");

        if(to != null)
            cmd.append("\"to\":\"" + to + "\", ");

        if(gas != null)
            cmd.append("\"gas\":\"" + gas + "\", ");

        if(gasPrice != null)
            cmd.append("\"gasPrice\":\"" + gasPrice +"\", ");

        if(value != null)
            cmd.append("\"value\":\"" + value + "\", ");

        if(nonce != null)
            cmd.append("\"nonce\":\"" + nonce + "\", ");

        if(data != null)
            cmd.append("\"data\":\"" + data);

        cmd.append("\"}]}");

        JSONObject jsonObject = new JSONObject(UscRpcExecutor.execute(cmd.toString()));

        String txId = jsonObject.get("result").toString();
        System.out.println(txId);

        Thread.sleep(1000);
        if (!Utils.isTransactionInMemPool(txId))
            sendTransaction(from, to, gas, gasPrice, value, data, nonce, --tries);

        while (!Utils.isTransactionMined(txId)) {
            if(!Utils.isTransactionInMemPool(txId))
                sendTransaction(from, to, gas, gasPrice, value, data, nonce, --tries);
            Thread.sleep(1000 * 10);
        }
        return true;
    }

    public static String getBlock() throws IOException {
        String cmd = "{" +
                "\"jsonrpc\":\"2.0\", " +
                "\"method\":\"eth_blockNumber\", " +
                "\"params\":[], " +
                "\"id\":83" +
                "}";
        return UscRpcExecutor.execute(cmd);
    }
}
