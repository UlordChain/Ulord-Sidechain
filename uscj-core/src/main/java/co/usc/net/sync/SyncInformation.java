package co.usc.net.sync;

import co.usc.net.BlockProcessResult;
import co.usc.net.MessageChannel;
import co.usc.net.NodeID;
import co.usc.scoring.EventType;
import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;

import javax.annotation.Nonnull;
import java.time.Instant;

public interface SyncInformation {
    boolean isKnownBlock(byte[] hash);

    boolean hasLowerDifficulty(NodeID nodeID);

    BlockProcessResult processBlock(Block block, MessageChannel channel);

    boolean blockHeaderIsValid(@Nonnull BlockHeader header);

    boolean blockHeaderIsValid(@Nonnull BlockHeader header, @Nonnull BlockHeader parentHeader);

    NodeID getSelectedPeerId();

    boolean hasGoodReputation(NodeID nodeID);

    void reportEvent(String message, EventType eventType, NodeID peerId, Object... arguments);

    int getScore(NodeID key);

    Instant getFailInstant(NodeID peerId);
}
