package tools;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeRegTestConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.RegTestParams;
import co.usc.ulordj.params.TestNet3Params;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static tools.Utils.getMinimumGasPrice;

// This is in testing phase. Once enough testing is done this will replace the original SyncUlordHeaders.java file.

public class SyncUlordHeaders1 implements Runnable {

    private long SYNC_DURATION = 1000 * 60 * 5; // 5 minutes so that other federations can sync together
    private BridgeConstants bridgeConstants;
    private NetworkParameters params;
    private String authorizedAddress;
    private String password;

    public SyncUlordHeaders1 (BridgeConstants bridgeConstants, String authorizedAddress, String password) {
        this.authorizedAddress = authorizedAddress;
        this.password = password;
        this.bridgeConstants = bridgeConstants;
        if(bridgeConstants instanceof BridgeTestNetConstants)
            this.params = TestNet3Params.get();
        else if(bridgeConstants instanceof BridgeRegTestConstants)
            this.params = RegTestParams.get();
        else
            this.params = MainNetParams.get();
    }

    @Override
    public void run() {
        syncUlordHeaders();
    }

    private void syncUlordHeaders() {

        while (true) {
            try {
                int chainHeight = DataDecoder.decodeGetUldBlockChainBestChainHeight(UscRpc.getUldBlockChainBestChainHeight()) + 1;  // Since zero index is genesis
                int ulordHeight = Integer.parseInt(UlordCli.getBlockCount(params));

                // Keep the ulord block headers in USC n blocks behind actual Ulord block headers.
                // to avoid ulord chain reorganization.
                if(params instanceof TestNet3Params)
                    ulordHeight -= 12;      // 30 minutes approx
                else if (params instanceof MainNetParams)
                    ulordHeight -= 144;     // 6 hours approx

                if(chainHeight > ulordHeight) {
                    Thread.sleep(SYNC_DURATION);
                    continue;
                }

                int  nblocks = bridgeConstants.getMaxUldHeadersPerUscBlock();
                String ulordBlockHash = UlordCli.getBlockHash(params, chainHeight);

                if((chainHeight + nblocks) > ulordHeight)
                    nblocks = ulordHeight - (chainHeight - 1);

                JSONArray ulordBlockHeaders = new JSONArray(UlordCli.getBlockHeaders(params, ulordBlockHash, nblocks, false));

                String[] headersList = ulordBlockHeaders.toList().toArray(new String[0]);

                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();

                System.out.println(dateFormat.format(date) + ": Syncing ulord headers from " + chainHeight + " to " + (chainHeight + headersList.length - 1));

                // Unlock account
                UscRpc.unlockAccount(authorizedAddress, password);

                sendSyncUlordHeadersTransaction(authorizedAddress, headersList, 1);

                System.out.println();

            } catch (InterruptedException in) {
                System.out.println("Sync Ulord headers thread closed " + in);
            } catch (IOException io) {
                System.out.println("RPC Exception: " + io);
            } finally {
                try {
                    Thread.sleep(SYNC_DURATION);
                } catch (InterruptedException iex) {
                    System.out.println("Sync Ulord headers thread closed " + iex);
                }
            }
        }
    }

    private static boolean sendSyncUlordHeadersTransaction(String authorizedAddress, String[] headers, int tries) throws InterruptedException, IOException {
        if (tries <= 0)
            return false;

        String sendTransactionResponse = UscRpc.sendTransaction(
                authorizedAddress,
                PrecompiledContracts.BRIDGE_ADDR_STR,
                "0x0",
                getMinimumGasPrice(),
                null,
                DataEncoder.encodeReceiveHeaders(headers),
                null);

        JSONObject jsonObject = new JSONObject(sendTransactionResponse);
        String txId = jsonObject.get("result").toString();

        System.out.println("SyncUlordHeaders Transaction ID: " + txId);

        Thread.sleep(1000 * 15);

        while (!Utils.isTransactionMined(txId)) {
            Thread.sleep(1000 * 15); // Sleep to stop flooding rpc requests.
            if(!Utils.isTransactionMined(txId)) // Check again because the transaction might have been mined after 15 seconds
                if (!Utils.isTransactionInMemPool(txId))
                    if(!sendSyncUlordHeadersTransaction(authorizedAddress, headers, --tries))
                        return false;
        }
        return true;
    }


}
