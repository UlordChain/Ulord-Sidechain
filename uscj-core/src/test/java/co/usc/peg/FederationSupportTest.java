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

import co.usc.config.BridgeConstants;
import org.ethereum.core.Block;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FederationSupportTest {

    private FederationSupport federationSupport;
    private BridgeConstants bridgeConstants;
    private BridgeStorageProvider provider;
    private Block executionBlock;

    @Before
    public void setUp() {
        provider = mock(BridgeStorageProvider.class);
        bridgeConstants = mock(BridgeConstants.class);
        executionBlock = mock(Block.class);
        federationSupport = new FederationSupport(provider, bridgeConstants, executionBlock);
    }

    @Test
    public void whenNewFederationIsNullThenActiveFederationIsGenesisFederation() {
        Federation genesisFederation = mock(Federation.class);
        when(provider.getNewFederation())
                .thenReturn(null);
        when(bridgeConstants.getGenesisFederation())
                .thenReturn(genesisFederation);

        assertThat(federationSupport.getActiveFederation(), is(genesisFederation));
    }

    @Test
    public void whenOldFederationIsNullThenActiveFederationIsNewFederation() {
        Federation newFederation = mock(Federation.class);
        when(provider.getNewFederation())
                .thenReturn(newFederation);
        when(provider.getOldFederation())
                .thenReturn(null);

        assertThat(federationSupport.getActiveFederation(), is(newFederation));
    }

    @Test
    public void whenOldAndNewFederationArePresentReturnOldFederationByActivationAge() {
        Federation newFederation = mock(Federation.class);
        Federation oldFederation = mock(Federation.class);
        when(provider.getNewFederation())
                .thenReturn(newFederation);
        when(provider.getOldFederation())
                .thenReturn(oldFederation);
        when(executionBlock.getNumber())
                .thenReturn(80L);
        when(bridgeConstants.getFederationActivationAge())
                .thenReturn(10L);
        when(newFederation.getCreationBlockNumber())
                .thenReturn(75L);

        assertThat(federationSupport.getActiveFederation(), is(oldFederation));
    }

    @Test
    public void whenOldAndNewFederationArePresentReturnNewFederationByActivationAge() {
        Federation newFederation = mock(Federation.class);
        Federation oldFederation = mock(Federation.class);
        when(provider.getNewFederation())
                .thenReturn(newFederation);
        when(provider.getOldFederation())
                .thenReturn(oldFederation);
        when(executionBlock.getNumber())
                .thenReturn(80L);
        when(bridgeConstants.getFederationActivationAge())
                .thenReturn(10L);
        when(newFederation.getCreationBlockNumber())
                .thenReturn(65L);

        assertThat(federationSupport.getActiveFederation(), is(newFederation));
    }
}