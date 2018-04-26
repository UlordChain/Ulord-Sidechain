package org.ethereum.util;

import co.usc.net.NodeID;
import co.usc.scoring.PeerScoring;
import co.usc.scoring.PeerScoringManager;
import org.ethereum.net.server.ChannelManager;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UscMockFactory {

    public static PeerScoringManager getPeerScoringManager() {
        PeerScoringManager peerScoringManager = mock(PeerScoringManager.class);
        when(peerScoringManager.hasGoodReputation(isA(NodeID.class))).thenReturn(true);
        when(peerScoringManager.getPeerScoring(isA(NodeID.class))).thenReturn(new PeerScoring());
        return peerScoringManager;
    }

    public static ChannelManager getChannelManager() {
        return mock(ChannelManager.class);
    }
}
