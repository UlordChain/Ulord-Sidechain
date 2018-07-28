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

    public static String getTransactionReceipt(String txId) throws IOException {
        String cmd = "{" +
                "\"jsonrpc\":\"2.0\", " +
                "\"method\":\"eth_getTransactionByHash\", " +
                "\"id\":\"881\", " +
                "\"params\":[" +
                "\"" + txId + "\"" +
                "]}";
        return UscRpcExecutor.execute(cmd);
    }

    public static String estimateGas(@Nullable String from,
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
        cmd.append("\"method\":\"eth_estimateGas\", ");
        cmd.append("\"params\":[{");

        if(from != null)
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

    public static String call(@Nullable String from,
                                         String to,
                                         @Nullable String gas,
                                         @Nullable String gasPrice,
                                         @Nullable String value,
                                         @Nullable String data,
                                         @Nullable String nonce,
                                                    String quantityTag)
            throws IOException {

        StringBuilder cmd = new StringBuilder();
        cmd.append("{");
        cmd.append("\"jsonrpc\":\"2.0\", ");
        cmd.append("\"id\":\"882\", ");
        cmd.append("\"method\":\"eth_call\", ");
        cmd.append("\"params\":[{");

        if(from != null)
            cmd.append("\"from\":\"" + from + "\", ");

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

        cmd.append("\"},\"" + quantityTag + "\"]}");

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

    public static String getFederationThreshold() throws IOException {
        return call(PrecompiledContracts.BRIDGE_ADDR_STR, DataEncoder.encodeGetFederationThreshold());
    }

    public static String gasPrice() throws IOException {
        String cmd = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_gasPrice\",\"params\":[],\"id\":886}";
        return UscRpcExecutor.execute(cmd);
    }

    public static String getBlockByNumber(String blockNumber, boolean fullTx) throws IOException {
        String cmd = "{\"jsonrpc\":\"2.0\", \"id\":887, \"method\":\"eth_getBlockByNumber\", \"params\":[\"" + blockNumber + "\", \"" + fullTx + "\"]}";
        return UscRpcExecutor.execute(cmd);
    }

    public static String getFederatorPublicKey(int index) throws IOException {
        return call(PrecompiledContracts.BRIDGE_ADDR_STR, DataEncoder.encodeGetFederatorPublicKey(index));
    }

    public static String getFederationSize() throws IOException {
        return call(PrecompiledContracts.BRIDGE_ADDR_STR, DataEncoder.encodeGetFederationSize());
    }

    public static String getRetiringFederationAddress() throws  IOException {
        return call(PrecompiledContracts.BRIDGE_ADDR_STR, DataEncoder.encodeGetRetiringFederationAddress());
    }

    public static String getRetiringFederationThreshold() throws  IOException {
        return call(PrecompiledContracts.BRIDGE_ADDR_STR, DataEncoder.encodeGetRetiringFederationThreshold());
    }

    public static String getRetiringFederationSize() throws IOException {
        return call(PrecompiledContracts.BRIDGE_ADDR_STR, DataEncoder.encodeGetRetiringFederationSize());
    }

    public static String getRetiringFederatorPublicKey(int index) throws IOException {
        return call(PrecompiledContracts.BRIDGE_ADDR_STR, DataEncoder.encodeGetRetiringFederatorPublicKey(index));
    }

    public static String getRetiringFederationCreationTime() throws IOException {
        return call(PrecompiledContracts.BRIDGE_ADDR_STR, DataEncoder.encodeGetRetiringFederationCreationTime());
    }

    public static String getRetiringFederationCreationBlockNumber() throws IOException {
        return call(PrecompiledContracts.BRIDGE_ADDR_STR, DataEncoder.encodeGetRetiringFederationCreationBlockNumber());
    }

    public static String getFederationCreationBlockNumber() throws IOException {
        return call(PrecompiledContracts.BRIDGE_ADDR_STR, DataEncoder.encodeGetFederationCreationBlockNumber());
    }

    public static String getFederationCreationTime() throws IOException {
        return call(PrecompiledContracts.BRIDGE_ADDR_STR, DataEncoder.encodeGetFederationCreationTime());
    }

    public static String getPendingFederationPublicKey(int index) throws IOException {
        return call(PrecompiledContracts.BRIDGE_ADDR_STR, DataEncoder.encodeGetPendingFederatorPublicKey(index));
    }

    public static String getFederationPublicKey(int index) throws IOException {
        return call(PrecompiledContracts.BRIDGE_ADDR_STR, DataEncoder.encodeGetFederatorPublicKey(index));
    }
}
