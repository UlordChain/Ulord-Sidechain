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

package co.usc.rpc.modules.txpool;

import co.usc.core.UscAddress;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.core.TransactionPool;
import org.ethereum.core.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

@Component
public class TxPoolModuleImpl implements TxPoolModule {

    public static final String PENDING = "pending";
    public static final String QUEUED = "queued";
    private final JsonNodeFactory jsonNodeFactory;
    private final TransactionPool transactionPool;

    @Autowired
    public TxPoolModuleImpl(TransactionPool transactionPool) {
        this.transactionPool = transactionPool;
        jsonNodeFactory = JsonNodeFactory.instance;
    }

    /**
     * This method should return 2 dictionaries containing pending and queued transactions
     * Each entry is an origin-address to a batch of scheduled transactions
     * These batches themselves are maps associating nonces with actual transactions.
     * When there are no transactions the answer would be
     * "{"pending": {}, "queued": {}}"
     */
    @Override
    public String content() {
        Map<String, JsonNode> contentProps = new HashMap<>();
        Map<UscAddress, Map<BigInteger, List<Transaction>>> pendingGrouped = groupTransactions(transactionPool.getPendingTransactions());
        Map<UscAddress, Map<BigInteger, List<Transaction>>> queuedGrouped = groupTransactions(transactionPool.getQueuedTransactions());
        contentProps.put(PENDING, serializeTransactions(pendingGrouped, this::fullSerializer));
        contentProps.put(QUEUED, serializeTransactions(queuedGrouped, this::fullSerializer));
        JsonNode node = jsonNodeFactory.objectNode().setAll(contentProps);
        return node.toString();
    }

    private JsonNode serializeTransactions(
            Map<UscAddress, Map<BigInteger, List<Transaction>>> groupedTransactions,
            Function<Transaction, JsonNode> txSerializer) {
        Map<String, JsonNode> senderProps = new HashMap<>();
        for (Map.Entry<UscAddress, Map<BigInteger, List<Transaction>>> entrySender : groupedTransactions.entrySet()){
            Map<String, JsonNode> nonceProps = new HashMap<>();
            for (Map.Entry<BigInteger, List<Transaction>> entryNonce : entrySender.getValue().entrySet()){
                ArrayNode txsNodes = jsonNodeFactory.arrayNode();
                for (Transaction tx : entryNonce.getValue()) {
                    txsNodes.add(txSerializer.apply(tx));
                }
                nonceProps.put(entryNonce.getKey().toString(),txsNodes);
            }
            senderProps.put(entrySender.getKey().toString(), jsonNodeFactory.objectNode().setAll(nonceProps));
        }
        return jsonNodeFactory.objectNode().setAll(senderProps);
    }

    private JsonNode fullSerializer(Transaction tx) {
        ObjectNode txNode = jsonNodeFactory.objectNode();
        txNode.put("blockhash", "0x0000000000000000000000000000000000000000000000000000000000000000");
        txNode.putNull("blocknumber");
        txNode.put("from", tx.getSender().toString());
        txNode.put("gas", tx.getGasLimitAsInteger().toString());
        txNode.put("gasPrice", tx.getGasPrice().toString());
        txNode.put("hash", tx.getHash().toHexString());
        txNode.put("input", tx.getData());
        txNode.put("nonce", tx.getNonceAsInteger().toString());
        txNode.put("to", tx.getReceiveAddress().toString());
        txNode.putNull("transactionIndex");
        txNode.put("value", tx.getValue().toString());

        return txNode;
    }

    private JsonNode summarySerializer(Transaction tx) {
        String summary = "{}: {} wei + {} x {} gas";
        String summaryFormatted = String.format(summary,
                tx.getReceiveAddress().toString(),
                tx.getValue().toString(),
                tx.getGasLimitAsInteger().toString(),
                tx.getGasPrice().toString());
        return jsonNodeFactory.textNode(summaryFormatted);
    }

    private Map<UscAddress, Map<BigInteger, List<Transaction>>> groupTransactions(List<Transaction> transactions) {
        Map<UscAddress, Map<BigInteger, List<Transaction>>> groupedTransactions = new HashMap<>();
        for (Transaction tx : transactions){
            Map<BigInteger, List<Transaction>> txsBySender = groupedTransactions.get(tx.getSender());
            if (txsBySender == null){
                txsBySender = new HashMap<>();
                groupedTransactions.put(tx.getSender(), txsBySender);
            }
            List<Transaction> txsByNonce = txsBySender.get(tx.getNonceAsInteger());
            if (txsByNonce == null){
                List<Transaction> txs = new ArrayList<>();
                txs.add(tx);
                txsBySender.put(tx.getNonceAsInteger(), txs);
            } else {
                txsByNonce.add(tx);
            }
        }
        return groupedTransactions;
    }

    /**
     * This method should return 2 dictionaries containing pending and queued transactions
     * Each entry is an origin-address to a batch of scheduled transactions
     * These batches themselves are maps associating nonces with transactions summary strings.
     * When there are no transactions the answer would be
     * "{"pending": {}, "queued": {}}"
     */
    @Override
    public String inspect() {
        Map<String, JsonNode> contentProps = new HashMap<>();
        Map<UscAddress, Map<BigInteger, List<Transaction>>> pendingGrouped = groupTransactions(transactionPool.getPendingTransactions());
        Map<UscAddress, Map<BigInteger, List<Transaction>>> queuedGrouped = groupTransactions(transactionPool.getQueuedTransactions());
        contentProps.put(PENDING, serializeTransactions(pendingGrouped, this::summarySerializer));
        contentProps.put(QUEUED, serializeTransactions(queuedGrouped, this::summarySerializer));
        JsonNode node = jsonNodeFactory.objectNode().setAll(contentProps);
        return node.toString();
    }

    /**
     * This method should return 2 integers for pending and queued transactions
     * These value represents
     * the number of transactions currently pending for inclusion in the next block(s),
     * as well as the ones that are being scheduled for future execution only.
     * "{"pending": 0, "queued": 0}"
     */
    @Override
    public String status() {
        Map<String, JsonNode> txProps = new HashMap<>();
        txProps.put(PENDING, jsonNodeFactory.numberNode(transactionPool.getPendingTransactions().size()));
        txProps.put(QUEUED, jsonNodeFactory.numberNode(transactionPool.getQueuedTransactions().size()));
        JsonNode node = jsonNodeFactory.objectNode().setAll(txProps);
        return node.toString();
    }
}