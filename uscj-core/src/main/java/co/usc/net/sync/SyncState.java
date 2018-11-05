package co.usc.net.sync;

import co.usc.net.MessageChannel;
import co.usc.net.messages.BodyResponseMessage;
import org.ethereum.core.BlockHeader;
import org.ethereum.core.BlockIdentifier;

import java.time.Duration;
import java.util.List;

public interface SyncState {
    void newBlockHeaders(List<BlockHeader> chunk);

    // TODO(mc) don't receive a full message
    void newBody(BodyResponseMessage message, MessageChannel peer);

    void newConnectionPointData(byte[] hash);

    /**
     * should only be called when a new peer arrives
     */
    void newPeerStatus();

    void newSkeleton(List<BlockIdentifier> skeletonChunk, MessageChannel peer);

    void onEnter();

    void tick(Duration duration);

    boolean isSyncing();
}
