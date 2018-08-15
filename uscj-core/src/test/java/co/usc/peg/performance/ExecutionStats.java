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

public class ExecutionStats {
    public String name;
    public Mean executionTimes;
    public Mean realExecutionTimes;
    public Mean slotsWritten;
    public Mean slotsCleared;

    public static long gasPerMicrosecond = 0;

    public ExecutionStats(String name) {
        this.name = name;
        this.executionTimes = new Mean();
        this.realExecutionTimes = new Mean();
        this.slotsWritten = new Mean();
        this.slotsCleared = new Mean();
    }

    public long getEstimatedGas() {
        return (executionTimes.getMean() / 1000) * gasPerMicrosecond;
    }

    public String getPrintable() {
        return String.format(
                "%-45s\tgas: %d\t\tcpu(us): %d\t\treal(us): %d\t\twrt(slots): %d\t\tclr(slots): %d",
                name,
                getEstimatedGas(),
                executionTimes.getMean() / 1000,
                realExecutionTimes.getMean() / 1000,
                slotsWritten.getMean(),
                slotsCleared.getMean()
        );
    }
}
