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

package co.usc.net.discovery.table;

import co.usc.net.NodeID;
import org.ethereum.net.rlpx.Node;

import java.util.Comparator;

/**
 * Created by mario on 22/02/17.
 */
public class NodeDistanceComparator implements Comparator<Node> {
    private NodeID targetNodeId;
    private transient DistanceCalculator calculator;

    public NodeDistanceComparator(NodeID targetNodeId, DistanceCalculator distanceCalculator) {
        this.targetNodeId = targetNodeId;
        this.calculator = distanceCalculator;
    }

    @Override
    public int compare(Node n1, Node n2) {
        int distance1 = calculator.calculateDistance(targetNodeId, n1.getId());
        int distance2 = calculator.calculateDistance(targetNodeId, n2.getId());

        return Integer.compare(distance1, distance2);
    }
}
