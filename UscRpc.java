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
        return UscRpcExecutor.execute(cmd);
    }

    public static String sendTransaction(String from,
                                          @Nullable String to,
                                          @Nullable String gas,
                                          @Nullable String gasPrice,
                                          @Nullable String value,
                                          @Nullable String data,
                                          @Nullable String nonce)
            throws IOException, InterruptedException {

//        if(tries < 0)
//            return "error:Transaction failed";

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

//        String txId = jsonObject.get("result").toString();
//
//        Thread.sleep(1000);
//        if (!Utils.isTransactionInMemPool(txId))
//            sendTransaction(from, to, gas, gasPrice, value, data, nonce, --tries);
//
//        while (!Utils.isTransactionMined(txId)) {
//            if(!Utils.isTransactionInMemPool(txId))
//                sendTransaction(from, to, gas, gasPrice, value, data, nonce, --tries);
//            Thread.sleep(1000 * 10);
//        }
        return jsonObject.toString();
    }

    public static String call(String to, String data) throws IOException {
        String cmd = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"eth_call\", " +
                "\"id\": \"1\", " +
                "\"params\": [{" +
                "\"to\": \"" + to + "\"," +
                "\"data\": \"" + data + "\"},\"latest\"]}";
        return UscRpcExecutor.execute(cmd);
    }

    public static String blockNumber() throws  IOException {
        String cmd = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"eth_blockNumber\", " +
                "\"id\": \"1\"," +
                "\"params\": []" +
                "}";
        return UscRpcExecutor.execute(cmd);
    }
}
