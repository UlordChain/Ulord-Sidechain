/*
 * This file is part of USC
 * Copyright (C) 2016 - 2018 USC developer team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.usc.peg.performance;

import co.usc.ulordj.core.*;
import co.usc.ulordj.crypto.TransactionSignature;
import co.usc.ulordj.script.Script;
import co.usc.ulordj.script.ScriptBuilder;
import co.usc.ulordj.script.ScriptChunk;
import co.usc.crypto.Keccak256;
import co.usc.peg.*;
import org.ethereum.core.Repository;
import org.ethereum.crypto.HashUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Ignore
public class AddSignatureTest extends BridgePerformanceTestCase {
    private UldTransaction releaseTx;
    private Keccak256 uscTxHash;
    private UldECKey federatorThatSignsKey;

    // Keys for the regtest genesis federation, which
    // we use for benchmarking this
    private static final List<UldECKey> federatorKeys = Arrays.asList(
            UldECKey.fromPrivate(HashUtil.keccak256("federator1".getBytes(StandardCharsets.UTF_8))),
            UldECKey.fromPrivate(HashUtil.keccak256("federator2".getBytes(StandardCharsets.UTF_8))),
            UldECKey.fromPrivate(HashUtil.keccak256("federator3".getBytes(StandardCharsets.UTF_8)))
    );

    @Test
    public void addSignature() {
        ExecutionStats stats = new ExecutionStats("addSignature");

        addSignature_nonFullySigned(100, stats);
        addSignature_fullySigned(100, stats);

        BridgePerformanceTest.addStats(stats);
    }

    private void addSignature_nonFullySigned(int times, ExecutionStats stats) {
        executeAndAverage(
                "addSignature-nonFullySigned",
                times,
                getABIEncoder(),
                getInitializerFor(0),
                Helper.getZeroValueRandomSenderTxBuilder(),
                Helper.getRandomHeightProvider(10),
                stats
        );
    }

    private void addSignature_fullySigned(int times, ExecutionStats stats) {
        executeAndAverage(
                "addSignature-fullySigned",
                times,
                getABIEncoder(),
                getInitializerFor(bridgeConstants.getGenesisFederation().getNumberOfSignaturesRequired()-1),
                Helper.getZeroValueRandomSenderTxBuilder(),
                Helper.getRandomHeightProvider(10),
                stats
        );
    }

    private ABIEncoder getABIEncoder() {
        return (int executionIndex) -> Bridge.ADD_SIGNATURE.encode(new Object[]{
                federatorThatSignsKey.getPubKey(),
                getSignaturesFor(releaseTx, federatorThatSignsKey),
                uscTxHash.getBytes()
            });
    }

    private BridgeStorageProviderInitializer getInitializerFor(int numSignatures) {
        final int minNumInputs = 1;
        final int maxNumInputs = 10;

        return (BridgeStorageProvider provider, Repository repository, int executionIndex) -> {
            releaseTx = new UldTransaction(networkParameters);

            Federation federation = bridgeConstants.getGenesisFederation();

            // Receiver and amounts
            UldECKey to = new UldECKey();
            Address toAddress = to.toAddress(networkParameters);
            Coin releaseAmount = Coin.CENT.multiply(Helper.randomInRange(10, 100));

            releaseTx.addOutput(releaseAmount, toAddress);

            // Input generation
            int numInputs = Helper.randomInRange(minNumInputs, maxNumInputs);
            for (int i = 0; i < numInputs; i++) {
                Coin inputAmount = releaseAmount.divide(numInputs);
                UldTransaction inputTx = new UldTransaction(networkParameters);
                inputTx.addOutput(inputAmount, federation.getAddress());
                releaseTx
                        .addInput(inputTx.getOutput(0))
                        .setScriptSig(PegTestUtils.createBaseInputScriptThatSpendsFromTheFederation(federation));
            }

            // Partial signing according to numSignatures asked for
            List<UldECKey> keysSelection = new ArrayList<>(federatorKeys);
            Collections.shuffle(keysSelection);
            int index = 0;
            int actualNumSignatures = Math.min(numSignatures, keysSelection.size()-1);
            while (index < actualNumSignatures) {
                signInputsWith(releaseTx, keysSelection.get(index));
                index++;
            }
            // Federator that needs to sign (method call)
            federatorThatSignsKey = keysSelection.get(index);

            // Random tx hash that we then use for the method call
            uscTxHash = new Keccak256(HashUtil.keccak256(BigInteger.valueOf(new Random().nextLong()).toByteArray()));

            // Get the tx into the txs waiting for signatures
            try {
                provider.getUscTxsWaitingForSignatures().put(uscTxHash, releaseTx);
            } catch (IOException e) {
                throw new RuntimeException("Exception while trying to gather txs waiting for signatures for storage initialization");
            }
        };
    }

    private List<byte[]> getSignaturesFor(UldTransaction tx, UldECKey key) {
        List<byte[]> signatures = new ArrayList<>();

        int inputIndex = 0;
        for (TransactionInput txIn : tx.getInputs()) {
            Script inputScript = txIn.getScriptSig();
            List<ScriptChunk> chunks = inputScript.getChunks();
            byte[] program = chunks.get(chunks.size() - 1).data;
            Script redeemScript = new Script(program);
            Sha256Hash sighash = tx.hashForSignature(inputIndex, redeemScript, UldTransaction.SigHash.ALL, false);
            UldECKey.ECDSASignature sig = key.sign(sighash);
            signatures.add(sig.encodeToDER());
            inputIndex++;
        }

        return signatures;
    }

    private void signInputsWith(UldTransaction tx, UldECKey key) {
        List<byte[]> signatures = getSignaturesFor(tx, key);

        for (int i = 0; i < tx.getInputs().size(); i++) {
            TransactionInput input = tx.getInput(i);
            Script inputScript = input.getScriptSig();
            List<ScriptChunk> chunks = inputScript.getChunks();
            byte[] program = chunks.get(chunks.size() - 1).data;
            Script redeemScript = new Script(program);
            Sha256Hash sighash = tx.hashForSignature(i, redeemScript, UldTransaction.SigHash.ALL, false);
            int sigIndex = inputScript.getSigInsertionIndex(sighash, key);
            UldECKey.ECDSASignature sig = UldECKey.ECDSASignature.decodeFromDER(signatures.get(i));
            TransactionSignature txSig = new TransactionSignature(sig, UldTransaction.SigHash.ALL, false);
            inputScript = ScriptBuilder.updateScriptWithSignature(inputScript, txSig.encodeToUlord(), sigIndex, 1, 1);
            input.setScriptSig(inputScript);
        }
    }
}
