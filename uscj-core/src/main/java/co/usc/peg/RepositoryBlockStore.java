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

package co.usc.peg;

import co.usc.ulordj.core.*;
import co.usc.ulordj.store.BlockStoreException;
import co.usc.core.UscAddress;
import co.usc.util.MaxSizeHashMap;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Repository;
import org.ethereum.vm.DataWord;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Implementation of a ulordj blockstore that persists to USC's Repository
 * @author Oscar Guindzberg
 */
public class RepositoryBlockStore implements UldBlockstoreWithCache {

    public static final String BLOCK_STORE_CHAIN_HEAD_KEY = "blockStoreChainHead";

    // power of 2 size that contains enough hashes to handle one year of blocks
    private static final int MAX_SIZE_MAP_STORED_BLOCKS = 65535;
    private static Map<Sha256Hash, StoredBlock> knownBlocks = new MaxSizeHashMap<>(MAX_SIZE_MAP_STORED_BLOCKS, false);

    private final Repository repository;
    private final UscAddress contractAddress;

    private final NetworkParameters params;

    public RepositoryBlockStore(SystemProperties config, Repository repository, UscAddress contractAddress) {
        this.repository = repository;
        this.contractAddress = contractAddress;

        // Insert the genesis block.
        try {
            this.params = config.getBlockchainConfig().getCommonConstants().getBridgeConstants().getUldParams();
            if (getChainHead()==null) {
                UldBlock genesisHeader = params.getGenesisBlock().cloneAsHeader();
                StoredBlock storedGenesis = new StoredBlock(genesisHeader, genesisHeader.getWork(), 0);
                put(storedGenesis);
                setChainHead(storedGenesis);
            }
        } catch (BlockStoreException e) {
            throw new RuntimeException(e);  // Cannot happen.
        } catch (VerificationException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    @Override
    public synchronized void put(StoredBlock block) throws BlockStoreException {
        Sha256Hash hash = block.getHeader().getHash();
        byte[] ba = storedBlockToByteArray(block);
        repository.addStorageBytes(contractAddress, new DataWord(hash.toString()), ba);
        knownBlocks.put(hash, block);
    }

    @Override
    public synchronized StoredBlock get(Sha256Hash hash) throws BlockStoreException {
        return getFromCache(hash);
    }

    public synchronized StoredBlock getFromCache(Sha256Hash hash) throws BlockStoreException {
        byte[] ba = repository.getStorageBytes(contractAddress, new DataWord(hash.toString()));
        if (ba==null) {
            return null;
        }

        StoredBlock storedBlock = knownBlocks.get(hash);
        if (storedBlock != null) {
            return storedBlock;
        }



        storedBlock = byteArrayToStoredBlock(ba);

        knownBlocks.put(hash, storedBlock);

        return storedBlock;
    }

    @Override
    public StoredBlock getChainHead() throws BlockStoreException {
        byte[] ba = repository.getStorageBytes(contractAddress, new DataWord(BLOCK_STORE_CHAIN_HEAD_KEY.getBytes(StandardCharsets.UTF_8)));
        if (ba==null) {
            return null;
        }
        StoredBlock storedBlock = byteArrayToStoredBlock(ba);
        return storedBlock;
    }

    @Override
    public void setChainHead(StoredBlock chainHead) throws BlockStoreException {
        byte[] ba = storedBlockToByteArray(chainHead);
        repository.addStorageBytes(contractAddress, new DataWord(BLOCK_STORE_CHAIN_HEAD_KEY.getBytes(StandardCharsets.UTF_8)), ba);
    }

    @Override
    public void close() {
    }

    @Override
    public NetworkParameters getParams() {
        return params;
    }

    private byte[] storedBlockToByteArray(StoredBlock block) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(188);
        block.serializeCompact(byteBuffer);
        byte[] ba = new byte[byteBuffer.position()];
        byteBuffer.flip();
        byteBuffer.get(ba);
        return ba;
    }

    private StoredBlock byteArrayToStoredBlock(byte[] ba) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(ba);
        return StoredBlock.deserializeCompact(params, byteBuffer);
    }



}
