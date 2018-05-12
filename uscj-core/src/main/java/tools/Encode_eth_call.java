package tools;

import co.usc.peg.Bridge;
import co.usc.ulordj.core.Sha256Hash;
import com.sun.istack.internal.NotNull;

import java.math.BigInteger;

public class Encode_eth_call {
    private static final String NOT_IMPLEMENTED =  "This function is not implemented yet!";
    private static final String NOT_FOUND = "Function not found!";

    public static void main(@NotNull String[] args) {
        if(args.length < 1) {
            System.out.println("Usage: <function name> <parameters>");
            return;
        }

        String command = args[0];
        String data = "";

        switch (command) {
            case "updateCollections":
                data = "This function is not implemented yet!";
                break;
            case "receiveHeaders":
                data = getReceiveHeadersString(args);
                break;
            case "registerUldTransaction":
                data = getRegisterUldTransactionString(args);
                break;
            case "releaseUld":
                data = NOT_IMPLEMENTED;
                break;
            case "addSignature":
                data = NOT_IMPLEMENTED;
                break;
            case "getStateForUldReleaseClient":
                data = NOT_IMPLEMENTED;
                break;
            case "getStateForDebugging":
                data = NOT_IMPLEMENTED;
                break;
            case "getUldBlockChainBestChainHeight":
                data = NOT_IMPLEMENTED;
                break;
            case "getUldBlockChainBlockLocator":
                data = NOT_IMPLEMENTED;
                break;
            case "getMinimumLockTxValue":
                data = NOT_IMPLEMENTED;
                break;
            case "isUldTxHashAlreadyProcessed":
                data = NOT_IMPLEMENTED;
                break;
            case "getUldTxHashProcessedHeight":
                data = NOT_IMPLEMENTED;
                break;
            case "getFederationAddress":
                data = NOT_IMPLEMENTED;
                break;
            case "getFederationSize":
                data = NOT_IMPLEMENTED;
                break;
            case "getFederationThreshold":
                data = NOT_IMPLEMENTED;
                break;

            case "addLockWhitelistAddress":
                data = getAddLockWhitelistAddressString(args);
                break;
            default:
                data = NOT_FOUND;
        }
        System.out.println(data);
    }

    private static String getReceiveHeadersString(String[] args) {
        if(args.length < 2)
            return "receiveHeaders <headers seperated by space>";

        byte[][] blocks = new byte[args.length - 1][140];
        for(int i = 0; i < args.length - 1; ++i) {
            byte[] b = Sha256Hash.hexStringToByteArray(args[i + 1]);
            blocks[i] = b;
        }
        return Sha256Hash.bytesToHex((Bridge.RECEIVE_HEADERS.encode(new Object[]{blocks})));
    }

    private static String getRegisterUldTransactionString(String[] args) {
        if(args.length < 3)
            return "registerUldTransaction <tx> <height> <merkletree>";

        byte[] tx = Sha256Hash.hexStringToByteArray(args[1]);
        Integer height = Integer.parseInt(args[2]);
        byte[] m = Sha256Hash.hexStringToByteArray(args[3]);
        return Sha256Hash.bytesToHex(Bridge.REGISTER_ULD_TRANSACTION.encode(new Object[]{tx, height, m}));
    }

    private static String getAddLockWhitelistAddressString(String[] args) {
        if(args.length < 3)
            return "addLockWhitelistAddress <address> <value>";

        BigInteger val = BigInteger.valueOf(Long.parseLong(args[2]));
        return Sha256Hash.bytesToHex(Bridge.ADD_LOCK_WHITELIST_ADDRESS.encode(new Object[]{args[1], val}));
    }
}

