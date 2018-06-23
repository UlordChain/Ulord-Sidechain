package tools;

import co.usc.peg.Bridge;
import org.ethereum.vm.PrecompiledContracts;
import org.spongycastle.util.encoders.Hex;

import javax.annotation.Nullable;
import java.io.IOException;

public class UscRpc {
    public static String unlockAccount(String address, String pwd) throws IOException {
        String cmd = "{" +
                "\"jsonrpc\":\"2.0\", " +
                "\"method\":\"personal_unlockAccount\", " +
                "\"id\":\"880\", " +
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
                "\"id\":\"881\", " +
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
            throws IOException {

        StringBuilder cmd = new StringBuilder();
        cmd.append("{");
        cmd.append("\"jsonrpc\":\"2.0\", ");
        cmd.append("\"id\":\"882\", ");
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

        return UscRpcExecutor.execute(cmd.toString());
    }

    public static String call(String to, String data) throws IOException {
        String cmd = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"eth_call\", " +
                "\"id\": \"883\", " +
                "\"params\": [{" +
                "\"to\": \"" + to + "\"," +
                "\"data\": \"" + data + "\"},\"latest\"]}";
        return UscRpcExecutor.execute(cmd);
    }

    public static String blockNumber() throws  IOException {
        String cmd = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"eth_blockNumber\", " +
                "\"id\": \"884\"," +
                "\"params\": []" +
                "}";
        return UscRpcExecutor.execute(cmd);
    }

    public static String getUldBlockChainBestChainHeight() throws IOException {
        return call(PrecompiledContracts.BRIDGE_ADDR_STR, Hex.toHexString(Bridge.GET_ULD_BLOCKCHAIN_BEST_CHAIN_HEIGHT.encodeSignature()));
    }

    public static String getTransactionCount(String address) throws IOException {
        String cmd = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"eth_getTransactionCount\", " +
                "\"id\": \"885\", " +
                "\"params\": [" +
                "\"" + address + "\", \"latest\"]}";
        return UscRpcExecutor.execute(cmd);
    }

    public static String getFederationAddress() throws IOException {
        return call(PrecompiledContracts.BRIDGE_ADDR_STR, Hex.toHexString(Bridge.GET_FEDERATION_ADDRESS.encodeSignature()));
    }

    public static String gasPrice() throws IOException {
        String cmd = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_gasPrice\",\"params\":[],\"id\":886}";
        return UscRpcExecutor.execute(cmd);
    }

}
