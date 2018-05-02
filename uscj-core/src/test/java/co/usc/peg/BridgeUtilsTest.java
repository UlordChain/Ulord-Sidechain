/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
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

package co.usc.peg;

import co.usc.ulordj.core.*;
import co.usc.ulordj.crypto.TransactionSignature;
import co.usc.ulordj.params.RegTestParams;
import co.usc.ulordj.script.Script;
import co.usc.ulordj.script.ScriptBuilder;
import co.usc.ulordj.wallet.CoinSelector;
import co.usc.ulordj.wallet.Wallet;
import co.usc.blockchain.utils.BlockGenerator;
import co.usc.config.BridgeRegTestConstants;
import co.usc.config.TestSystemProperties;
import co.usc.core.UscAddress;
import co.usc.peg.ulord.UscAllowUnconfirmedCoinSelector;
import org.ethereum.config.blockchain.RegTestConfig;
import org.ethereum.core.Block;
import org.ethereum.core.CallTransaction;
import org.ethereum.core.Genesis;
import org.ethereum.vm.PrecompiledContracts;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BridgeUtilsTest {

    private static final Logger logger = LoggerFactory.getLogger("test");

    private static final String TO_ADDRESS = "0000000000000000000000000000000000000006";
    private static final BigInteger AMOUNT = new BigInteger("1");
    private static final BigInteger NONCE = new BigInteger("0");
    private static final BigInteger GAS_PRICE = new BigInteger("100");
    private static final BigInteger GAS_LIMIT = new BigInteger("1000");
    private static final String DATA = "80af2871";
    private TestSystemProperties config;

    @Before
    public void setupConfig(){
        config = new TestSystemProperties();
    }

    @Test
    public void testIsLock() throws Exception {
        // Lock is for the genesis federation ATM
        NetworkParameters params = RegTestParams.get();
        Context uldContext = new Context(params);
        BridgeRegTestConstants bridgeConstants = BridgeRegTestConstants.getInstance();
        Federation federation = bridgeConstants.getGenesisFederation();
        Wallet wallet = new BridgeUldWallet(uldContext, Arrays.asList(federation));
        wallet.addWatchedAddress(federation.getAddress(), federation.getCreationTime().toEpochMilli());
        Address address = federation.getAddress();

        // Tx sending less than 1 uld to the federation, not a lock tx
        UldTransaction tx = new UldTransaction(params);
        tx.addOutput(Coin.CENT, address);
        tx.addInput(Sha256Hash.ZERO_HASH, 0, new Script(new byte[]{}));
        assertFalse(BridgeUtils.isLockTx(tx, federation, uldContext, bridgeConstants));

        // Tx sending 1 uld to the federation, but also spending from the federation addres, the typical release tx, not a lock tx.
        UldTransaction tx2 = new UldTransaction(params);
        tx2.addOutput(Coin.COIN, address);
        TransactionInput txIn = new TransactionInput(params, tx2, new byte[]{}, new TransactionOutPoint(params, 0, Sha256Hash.ZERO_HASH));
        tx2.addInput(txIn);
        signWithNecessaryKeys(bridgeConstants.getGenesisFederation(), bridgeConstants.getFederatorPrivateKeys(), txIn, tx2, bridgeConstants);
        assertFalse(BridgeUtils.isLockTx(tx2, federation, uldContext, bridgeConstants));

        // Tx sending 1 uld to the federation, is a lock tx
        UldTransaction tx3 = new UldTransaction(params);
        tx3.addOutput(Coin.COIN, address);
        tx3.addInput(Sha256Hash.ZERO_HASH, 0, new Script(new byte[]{}));
        assertTrue(BridgeUtils.isLockTx(tx3, federation, uldContext, bridgeConstants));

        // Tx sending 50 uld to the federation, is a lock tx
        UldTransaction tx4 = new UldTransaction(params);
        tx4.addOutput(Coin.FIFTY_COINS, address);
        tx4.addInput(Sha256Hash.ZERO_HASH, 0, new Script(new byte[]{}));
        assertTrue(BridgeUtils.isLockTx(tx4, federation, uldContext, bridgeConstants));
    }

    @Test
    public void testIsLockForTwoFederations() throws Exception {
        BridgeRegTestConstants bridgeConstants = BridgeRegTestConstants.getInstance();
        NetworkParameters parameters = bridgeConstants.getUldParams();
        Context uldContext = new Context(parameters);

        List<UldECKey> federation1Keys = Arrays.asList(new UldECKey[]{
                UldECKey.fromPrivate(Hex.decode("fa01")),
                UldECKey.fromPrivate(Hex.decode("fa02")),
        });
        federation1Keys.sort(UldECKey.PUBKEY_COMPARATOR);
        Federation federation1 = new Federation(federation1Keys, Instant.ofEpochMilli(1000L), 0L, parameters);

        List<UldECKey> federation2Keys = Arrays.asList(new UldECKey[]{
                UldECKey.fromPrivate(Hex.decode("fb01")),
                UldECKey.fromPrivate(Hex.decode("fb02")),
                UldECKey.fromPrivate(Hex.decode("fb03")),
        });
        federation2Keys.sort(UldECKey.PUBKEY_COMPARATOR);
        Federation federation2 = new Federation(federation2Keys, Instant.ofEpochMilli(2000L), 0L, parameters);

        Address address1 = federation1.getAddress();
        Address address2 = federation2.getAddress();

        List<Federation> federations = Arrays.asList(federation1, federation2);
        List<Address> addresses = Arrays.asList(address1, address2);

        // Tx sending less than 1 uld to the first federation, not a lock tx
        UldTransaction tx = new UldTransaction(parameters);
        tx.addOutput(Coin.CENT, address1);
        tx.addInput(Sha256Hash.ZERO_HASH, 0, new Script(new byte[]{}));
        assertFalse(BridgeUtils.isLockTx(tx, federations, uldContext, bridgeConstants));

        // Tx sending less than 1 uld to the second federation, not a lock tx
        tx = new UldTransaction(parameters);
        tx.addOutput(Coin.CENT, address2);
        tx.addInput(Sha256Hash.ZERO_HASH, 0, new Script(new byte[]{}));
        assertFalse(BridgeUtils.isLockTx(tx, federations, uldContext, bridgeConstants));

        // Tx sending less than 1 uld to both federations, not a lock tx
        tx = new UldTransaction(parameters);
        tx.addOutput(Coin.CENT, address1);
        tx.addOutput(Coin.CENT, address2);
        tx.addInput(Sha256Hash.ZERO_HASH, 0, new Script(new byte[]{}));
        assertFalse(BridgeUtils.isLockTx(tx, federations, uldContext, bridgeConstants));

        // Tx sending 1 uld to the first federation, but also spending from the first federation address, the typical release tx, not a lock tx.
        UldTransaction tx2 = new UldTransaction(parameters);
        tx2.addOutput(Coin.COIN, address1);
        TransactionInput txIn = new TransactionInput(parameters, tx2, new byte[]{}, new TransactionOutPoint(parameters, 0, Sha256Hash.ZERO_HASH));
        tx2.addInput(txIn);
        signWithNecessaryKeys(federation1, federation1Keys, txIn, tx2, bridgeConstants);
        assertFalse(BridgeUtils.isLockTx(tx2, federations, uldContext, bridgeConstants));

        // Tx sending 1 uld to the second federation, but also spending from the second federation address, the typical release tx, not a lock tx.
        tx2 = new UldTransaction(parameters);
        tx2.addOutput(Coin.COIN, address2);
        txIn = new TransactionInput(parameters, tx2, new byte[]{}, new TransactionOutPoint(parameters, 0, Sha256Hash.ZERO_HASH));
        tx2.addInput(txIn);
        signWithNecessaryKeys(federation2, federation2Keys, txIn, tx2, bridgeConstants);
        assertFalse(BridgeUtils.isLockTx(tx2, federations, uldContext, bridgeConstants));

        // Tx sending 1 uld to both federations, but also spending from the first federation address, the typical release tx, not a lock tx.
        tx2 = new UldTransaction(parameters);
        tx2.addOutput(Coin.COIN, address1);
        tx2.addOutput(Coin.COIN, address2);
        txIn = new TransactionInput(parameters, tx2, new byte[]{}, new TransactionOutPoint(parameters, 0, Sha256Hash.ZERO_HASH));
        tx2.addInput(txIn);
        signWithNecessaryKeys(federation1, federation1Keys, txIn, tx2, bridgeConstants);
        assertFalse(BridgeUtils.isLockTx(tx2, federations, uldContext, bridgeConstants));

        // Tx sending 1 uld to both federations, but also spending from the second federation address, the typical release tx, not a lock tx.
        tx2 = new UldTransaction(parameters);
        tx2.addOutput(Coin.COIN, address1);
        tx2.addOutput(Coin.COIN, address2);
        txIn = new TransactionInput(parameters, tx2, new byte[]{}, new TransactionOutPoint(parameters, 0, Sha256Hash.ZERO_HASH));
        tx2.addInput(txIn);
        signWithNecessaryKeys(federation2, federation2Keys, txIn, tx2, bridgeConstants);
        assertFalse(BridgeUtils.isLockTx(tx2, federations, uldContext, bridgeConstants));

        // Tx sending 1 uld to the first federation, is a lock tx
        UldTransaction tx3 = new UldTransaction(parameters);
        tx3.addOutput(Coin.COIN, address1);
        tx3.addInput(Sha256Hash.ZERO_HASH, 0, new Script(new byte[]{}));
        assertTrue(BridgeUtils.isLockTx(tx3, federations, uldContext, bridgeConstants));

        // Tx sending 1 uld to the second federation, is a lock tx
        tx3 = new UldTransaction(parameters);
        tx3.addOutput(Coin.COIN, address2);
        tx3.addInput(Sha256Hash.ZERO_HASH, 0, new Script(new byte[]{}));
        assertTrue(BridgeUtils.isLockTx(tx3, federations, uldContext, bridgeConstants));

        // Tx sending 1 uld to the both federations, is a lock tx
        tx3 = new UldTransaction(parameters);
        tx3.addOutput(Coin.COIN, address1);
        tx3.addOutput(Coin.COIN, address2);
        tx3.addInput(Sha256Hash.ZERO_HASH, 0, new Script(new byte[]{}));
        assertTrue(BridgeUtils.isLockTx(tx3, federations, uldContext, bridgeConstants));

        // Tx sending 50 uld to the first federation, is a lock tx
        UldTransaction tx4 = new UldTransaction(parameters);
        tx4.addOutput(Coin.FIFTY_COINS, address1);
        tx4.addInput(Sha256Hash.ZERO_HASH, 0, new Script(new byte[]{}));
        assertTrue(BridgeUtils.isLockTx(tx4, federations, uldContext, bridgeConstants));

        // Tx sending 50 uld to the second federation, is a lock tx
        tx4 = new UldTransaction(parameters);
        tx4.addOutput(Coin.FIFTY_COINS, address2);
        tx4.addInput(Sha256Hash.ZERO_HASH, 0, new Script(new byte[]{}));
        assertTrue(BridgeUtils.isLockTx(tx4, federations, uldContext, bridgeConstants));

        // Tx sending 50 uld to the both federations, is a lock tx
        tx4 = new UldTransaction(parameters);
        tx4.addOutput(Coin.FIFTY_COINS, address1);
        tx4.addOutput(Coin.FIFTY_COINS, address2);
        tx4.addInput(Sha256Hash.ZERO_HASH, 0, new Script(new byte[]{}));
        assertTrue(BridgeUtils.isLockTx(tx4, federations, uldContext, bridgeConstants));
    }

    @Test
    public void testIsMigrationTx() {
        BridgeRegTestConstants bridgeConstants = BridgeRegTestConstants.getInstance();
        NetworkParameters parameters = bridgeConstants.getUldParams();
        Context uldContext = new Context(parameters);

        List<UldECKey> activeFederationKeys = Stream.of(
            UldECKey.fromPrivate(Hex.decode("fa01")),
            UldECKey.fromPrivate(Hex.decode("fa02"))
        ).sorted(UldECKey.PUBKEY_COMPARATOR).collect(Collectors.toList());
        Federation activeFederation = new Federation(activeFederationKeys, Instant.ofEpochMilli(2000L), 2L, parameters);

        List<UldECKey> retiringFederationKeys = Stream.of(
                UldECKey.fromPrivate(Hex.decode("fb01")),
                UldECKey.fromPrivate(Hex.decode("fb02")),
                UldECKey.fromPrivate(Hex.decode("fb03"))
        ).sorted(UldECKey.PUBKEY_COMPARATOR).collect(Collectors.toList());
        Federation retiringFederation = new Federation(retiringFederationKeys, Instant.ofEpochMilli(1000L), 1L, parameters);

        Address activeFederationAddress = activeFederation.getAddress();

        UldTransaction migrationTx = new UldTransaction(parameters);
        migrationTx.addOutput(Coin.COIN, activeFederationAddress);
        TransactionInput migrationTxInput = new TransactionInput(parameters, migrationTx, new byte[]{}, new TransactionOutPoint(parameters, 0, Sha256Hash.ZERO_HASH));
        migrationTx.addInput(migrationTxInput);
        signWithNecessaryKeys(retiringFederation, retiringFederationKeys, migrationTxInput, migrationTx, bridgeConstants);
        assertThat(BridgeUtils.isMigrationTx(migrationTx, activeFederation, retiringFederation, uldContext, bridgeConstants), is(true));

        UldTransaction toActiveFederationTx = new UldTransaction(parameters);
        toActiveFederationTx.addOutput(Coin.COIN, activeFederationAddress);
        toActiveFederationTx.addInput(Sha256Hash.ZERO_HASH, 0, new Script(new byte[]{}));
        assertThat(BridgeUtils.isMigrationTx(toActiveFederationTx, activeFederation, retiringFederation, uldContext, bridgeConstants), is(false));

        Address randomAddress = Address.fromBase58(
            NetworkParameters.fromID(NetworkParameters.ID_REGTEST),
            "n3PLxDiwWqa5uH7fSbHCxS6VAjD9Y7Rwkj"
        );
        UldTransaction fromRetiringFederationTx = new UldTransaction(parameters);
        fromRetiringFederationTx.addOutput(Coin.COIN, randomAddress);
        TransactionInput fromRetiringFederationTxInput = new TransactionInput(parameters, fromRetiringFederationTx, new byte[]{}, new TransactionOutPoint(parameters, 0, Sha256Hash.ZERO_HASH));
        fromRetiringFederationTx.addInput(fromRetiringFederationTxInput);
        signWithNecessaryKeys(retiringFederation, retiringFederationKeys, fromRetiringFederationTxInput, fromRetiringFederationTx, bridgeConstants);
        assertThat(BridgeUtils.isMigrationTx(fromRetiringFederationTx, activeFederation, retiringFederation, uldContext, bridgeConstants), is(false));

        assertThat(BridgeUtils.isMigrationTx(migrationTx, activeFederation, null, uldContext, bridgeConstants), is(false));
    }

    @Test
    public void getAddressFromEthTransaction() {
        org.ethereum.core.Transaction tx = org.ethereum.core.Transaction.create(config, TO_ADDRESS, AMOUNT, NONCE, GAS_PRICE, GAS_LIMIT, DATA);
        byte[] privKey = generatePrivKey();
        tx.sign(privKey);

        Address expectedAddress = UldECKey.fromPrivate(privKey).toAddress(RegTestParams.get());
        Address result = BridgeUtils.recoverUldAddressFromEthTransaction(tx, RegTestParams.get());

        assertEquals(expectedAddress, result);
    }

    @Test(expected = Exception.class)
    public void getAddressFromEthNotSignTransaction() {
        org.ethereum.core.Transaction tx = org.ethereum.core.Transaction.create(config, TO_ADDRESS, AMOUNT, NONCE, GAS_PRICE, GAS_LIMIT, DATA);
        BridgeUtils.recoverUldAddressFromEthTransaction(tx, RegTestParams.get());
    }

    private byte[] generatePrivKey() {
        SecureRandom random = new SecureRandom();
        byte[] privKey = new byte[32];
        random.nextBytes(privKey);
        return privKey;
    }

    private void signWithNecessaryKeys(Federation federation, List<UldECKey> privateKeys, TransactionInput txIn, UldTransaction tx, BridgeRegTestConstants bridgeConstants) {
        Script redeemScript = PegTestUtils.createBaseRedeemScriptThatSpendsFromTheFederation(federation);
        Script inputScript = PegTestUtils.createBaseInputScriptThatSpendsFromTheFederation(federation);
        txIn.setScriptSig(inputScript);

        Sha256Hash sighash = tx.hashForSignature(0, redeemScript, UldTransaction.SigHash.ALL, false);

        for (int i = 0; i < federation.getNumberOfSignaturesRequired(); i++) {
            inputScript = signWithOneKey(federation, privateKeys, inputScript, sighash, i, bridgeConstants);
        }
        txIn.setScriptSig(inputScript);
    }

    private Script signWithOneKey(Federation federation, List<UldECKey> privateKeys, Script inputScript, Sha256Hash sighash, int federatorIndex, BridgeRegTestConstants bridgeConstants) {
        UldECKey federatorPrivKey = privateKeys.get(federatorIndex);
        UldECKey federatorPublicKey = federation.getPublicKeys().get(federatorIndex);

        UldECKey.ECDSASignature sig = federatorPrivKey.sign(sighash);
        TransactionSignature txSig = new TransactionSignature(sig, UldTransaction.SigHash.ALL, false);

        int sigIndex = inputScript.getSigInsertionIndex(sighash, federatorPublicKey);
        inputScript = ScriptBuilder.updateScriptWithSignature(inputScript, txSig.encodeToUlord(), sigIndex, 1, 1);
        return inputScript;
    }

    @Test
    public void isFreeBridgeTxTrue() {
        config.setBlockchainConfig(new UnitTestBlockchainNetConfig());
        isFreeBridgeTx(true, PrecompiledContracts.BRIDGE_ADDR, BridgeRegTestConstants.getInstance().getFederatorPrivateKeys().get(0).getPrivKeyBytes());
    }

    @Test
    public void isFreeBridgeTxOtherContract() {
        config.setBlockchainConfig(new UnitTestBlockchainNetConfig());
        isFreeBridgeTx(false, PrecompiledContracts.IDENTITY_ADDR, BridgeRegTestConstants.getInstance().getFederatorPrivateKeys().get(0).getPrivKeyBytes());
    }

    @Test
    public void isFreeBridgeTxFreeTxDisabled() {
        config.setBlockchainConfig(new RegTestConfig() {
            @Override
            public boolean areBridgeTxsFree() {
                return false;
            }
        });
        isFreeBridgeTx(false, PrecompiledContracts.BRIDGE_ADDR, BridgeRegTestConstants.getInstance().getFederatorPrivateKeys().get(0).getPrivKeyBytes());
    }

    @Test
    public void isFreeBridgeTxNonFederatorKey() {
        config.setBlockchainConfig(new UnitTestBlockchainNetConfig());
        isFreeBridgeTx(false, PrecompiledContracts.BRIDGE_ADDR, new UldECKey().getPrivKeyBytes());
    }

    @Test
    public void getFederationNoSpendWallet() {
        NetworkParameters regTestParameters = NetworkParameters.fromID(NetworkParameters.ID_REGTEST);
        Federation federation = new Federation(Arrays.asList(new UldECKey[]{
                UldECKey.fromPublicOnly(Hex.decode("036bb9eab797eadc8b697f0e82a01d01cabbfaaca37e5bafc06fdc6fdd38af894a")),
                UldECKey.fromPublicOnly(Hex.decode("031da807c71c2f303b7f409dd2605b297ac494a563be3b9ca5f52d95a43d183cc5"))
        }), Instant.ofEpochMilli(5005L), 0L, regTestParameters);
        Context mockedUldContext = mock(Context.class);
        when(mockedUldContext.getParams()).thenReturn(regTestParameters);

        Wallet wallet = BridgeUtils.getFederationNoSpendWallet(mockedUldContext, federation);
        Assert.assertEquals(BridgeUldWallet.class, wallet.getClass());
        assertIsWatching(federation.getAddress(), wallet, regTestParameters);
    }

    @Test
    public void getFederationSpendWallet() throws UTXOProviderException {
        NetworkParameters regTestParameters = NetworkParameters.fromID(NetworkParameters.ID_REGTEST);
        Federation federation = new Federation(Arrays.asList(new UldECKey[]{
                UldECKey.fromPublicOnly(Hex.decode("036bb9eab797eadc8b697f0e82a01d01cabbfaaca37e5bafc06fdc6fdd38af894a")),
                UldECKey.fromPublicOnly(Hex.decode("031da807c71c2f303b7f409dd2605b297ac494a563be3b9ca5f52d95a43d183cc5"))
        }), Instant.ofEpochMilli(5005L), 0L, regTestParameters);
        Context mockedUldContext = mock(Context.class);
        when(mockedUldContext.getParams()).thenReturn(regTestParameters);

        List<UTXO> mockedUtxos = new ArrayList<>();
        mockedUtxos.add(mock(UTXO.class));
        mockedUtxos.add(mock(UTXO.class));
        mockedUtxos.add(mock(UTXO.class));

        Wallet wallet = BridgeUtils.getFederationSpendWallet(mockedUldContext, federation, mockedUtxos);
        Assert.assertEquals(BridgeUldWallet.class, wallet.getClass());
        assertIsWatching(federation.getAddress(), wallet, regTestParameters);
        CoinSelector selector = wallet.getCoinSelector();
        Assert.assertEquals(UscAllowUnconfirmedCoinSelector.class, selector.getClass());
        UTXOProvider utxoProvider = wallet.getUTXOProvider();
        Assert.assertEquals(UscUTXOProvider.class, utxoProvider.getClass());
        Assert.assertEquals(mockedUtxos, utxoProvider.getOpenTransactionOutputs(Collections.emptyList()));
    }

    private void assertIsWatching(Address address, Wallet wallet, NetworkParameters parameters) {
        List<Script> watchedScripts = wallet.getWatchedScripts();
        Assert.assertEquals(1, watchedScripts.size());
        Script watchedScript = watchedScripts.get(0);
        Assert.assertTrue(watchedScript.isPayToScriptHash());
        Assert.assertEquals(address.toString(), watchedScript.getToAddress(parameters).toString());
    }


    private void isFreeBridgeTx(boolean expected, UscAddress destinationAddress, byte[] privKeyBytes) {
        Bridge bridge = new Bridge(config, PrecompiledContracts.BRIDGE_ADDR);

        org.ethereum.core.Transaction rskTx = CallTransaction.createCallTransaction(
                config, 0,
                1,
                1,
                destinationAddress,
                0,
                Bridge.UPDATE_COLLECTIONS);
        rskTx.sign(privKeyBytes);

        Block rskExecutionBlock = new BlockGenerator().createChildBlock(Genesis.getInstance(config));
        bridge.init(rskTx, rskExecutionBlock, null, null, null, null);
        Assert.assertEquals(expected, BridgeUtils.isFreeBridgeTx(config, rskTx, rskExecutionBlock.getNumber()));
    }
}
