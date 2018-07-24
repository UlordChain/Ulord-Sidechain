package tools;

import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.TestNet3Params;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static tools.Utils.getMinimumGasPrice;


// This is in testing phase. Once enough testing is done this will replace the original SyncUlordHeaders.java file.

public class SyncUlordHeaders1 {
    private static long SYNC_DURATION = 1000 * 60 * 1; // every minute
    private static int MAX_BLOCKS_PER_TX = 100;
    private static NetworkParameters params = TestNet3Params.get();

    public static void main(String[] args) {
        syncUlordHeaders("a13d7dbabac37d9b756f573ecd7c0e652ff043c5","abcd1234");
    }

    private static void syncUlordHeaders(String authorizedAddress, String password) {
        try {
            while (true) {
                int chainHeight = DataDecoder.decodeGetUldBlockChainBestChainHeight(UscRpc.getUldBlockChainBestChainHeight()) + 1;  // Since zero index is genesis
                int ulordHeight = Integer.parseInt(UlordCli.getBlockCount(params));

                if(params instanceof TestNet3Params)
                    ulordHeight -= 12;
                else if (params instanceof MainNetParams)
                    ulordHeight -= 144;

                StringBuilder builder = new StringBuilder();
                String line = null;
                int counter = 0;
                for (int i = 0; i < MAX_BLOCKS_PER_TX; ++i) {

                    if((chainHeight + i) > ulordHeight)
                        break;

                    //getBlockHash gets the block hash for the given height.
                    line = UlordCli.getBlockHash(params, chainHeight + i);

                    //getBlockHeader gets the block header for the given block hash.
                    line = UlordCli.getBlockHeader(params, line, false);

                    if (line != null) {
                        builder.append(line);
                        builder.append(" ");
                    }
                    counter++;
                }

                if(builder.length() == 0) {
                    Thread.sleep(SYNC_DURATION);
                    continue;
                }

                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();

                System.out.println(dateFormat.format(date) + ": Syncing ulord headers from " + chainHeight + " to " + (chainHeight + counter - 1));
                // Unlock account
                UscRpc.unlockAccount(authorizedAddress, password);

                sendSyncUlordHeadersTransaction(authorizedAddress, builder, 1);

                System.out.println();
                Thread.sleep(SYNC_DURATION);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private static boolean sendSyncUlordHeadersTransaction(String authorizedAddress, StringBuilder builder, int tries) throws InterruptedException, IOException {
        if (tries <= 0)
            return false;

        String sendTransactionResponse = UscRpc.sendTransaction(
                authorizedAddress,
                PrecompiledContracts.BRIDGE_ADDR_STR,
                "0x0",
                getMinimumGasPrice(),
                null,
                DataEncoder.encodeReceiveHeaders(builder.toString().split(" ")),
                null);

        JSONObject jsonObject = new JSONObject(sendTransactionResponse);
        String txId = jsonObject.get("result").toString();

        System.out.println("SyncUlordHeaders Transaction ID: " + txId);

        Thread.sleep(1000 * 15);

        while (!Utils.isTransactionMined(txId)) {
            Thread.sleep(1000 * 15); // Sleep to stop flooding rpc requests.
            if(!Utils.isTransactionMined(txId)) // Check again because the transaction might have been mined after 15 seconds
                if (!Utils.isTransactionInMemPool(txId))
                    if(!sendSyncUlordHeadersTransaction(authorizedAddress, builder, --tries))
                        return false;
        }
        return true;
    }
}
