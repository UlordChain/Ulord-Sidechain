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

package co.usc.test.builders;

import co.usc.blockchain.utils.BlockGenerator;
import co.usc.config.TestSystemProperties;
import co.usc.core.bc.BlockChainImpl;
import co.usc.core.bc.BlockExecutor;
import co.usc.test.World;
import co.usc.blockchain.utils.BlockGenerator;
import co.usc.config.TestSystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionExecutor;
import org.ethereum.vm.PrecompiledContracts;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.spongycastle.util.BigIntegers;

import java.math.BigInteger;
import java.util.List;

/**
 * Created by ajlopez on 8/6/2016.
 */
public class BlockBuilder {
    private BlockChainImpl blockChain;
    private final BlockGenerator blockGenerator;
    private Block parent;
    private long difficulty;
    private List<Transaction> txs;
    private List<BlockHeader> uncles;
    private BigInteger minGasPrice;
    private byte[] gasLimit;

    public BlockBuilder() {
        this.blockGenerator = new BlockGenerator();
    }

    public BlockBuilder(World world) {
        this(world.getBlockChain(), new BlockGenerator());
    }

    public BlockBuilder(BlockChainImpl blockChain) {
        this(blockChain, new BlockGenerator());
    }

    public BlockBuilder(BlockChainImpl blockChain, BlockGenerator blockGenerator) {
        this.blockChain = blockChain;
        this.blockGenerator = blockGenerator;
        // sane defaults
        this.parent(blockChain.getBestBlock());
    }

    public BlockBuilder parent(Block parent) {
        this.parent = parent;
        this.gasLimit = parent.getGasLimit();
        return this;
    }

    public BlockBuilder difficulty(long difficulty) {
        this.difficulty = difficulty;
        return this;
    }

    public BlockBuilder transactions(List<Transaction> txs) {
        this.txs = txs;
        return this;
    }

    public BlockBuilder uncles(List<BlockHeader> uncles) {
        this.uncles = uncles;
        return this;
    }

    public BlockBuilder minGasPrice(BigInteger minGasPrice) {
        this.minGasPrice = minGasPrice;
        return this;
    }

    /**
     * This has to be called after .parent() in order to have any effect
     */
    public BlockBuilder gasLimit(BigInteger gasLimit) {
        this.gasLimit = BigIntegers.asUnsignedByteArray(gasLimit);
        return this;
    }

    public Block build() {
        Block block = blockGenerator.createChildBlock(parent, txs, uncles, difficulty, this.minGasPrice, gasLimit);

        if (blockChain != null) {
            final ProgramInvokeFactoryImpl programInvokeFactory = new ProgramInvokeFactoryImpl();
            final TestSystemProperties config = new TestSystemProperties();
            BlockExecutor executor = new BlockExecutor(blockChain.getRepository(), (tx1, txindex1, coinbase, track1, block1, totalGasUsed1) -> new TransactionExecutor(
                    tx1,
                    txindex1,
                    block1.getCoinbase(),
                    track1,
                    blockChain.getBlockStore(),
                    null,
                    programInvokeFactory,
                    block1,
                    blockChain.getListener(),
                    totalGasUsed1,
                    config.getVmConfig(),
                    config.getBlockchainConfig(),
                    config.playVM(),
                    config.isRemascEnabled(),
                    config.vmTrace(),
                    new PrecompiledContracts(config),
                    config.databaseDir(),
                    config.vmTraceDir(),
                    config.vmTraceCompressed()
            ));
            executor.executeAndFill(block, parent);
        }

        return block;
    }
}
