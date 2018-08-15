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

package co.usc.config;

import co.usc.core.UscAddress;

/**
 * Wraps configuration for Mining, which is usually derived from configuration files.
 */
public class MiningConfig {
    private final UscAddress coinbaseAddress;
    private final double minFeesNotifyInDollars;
    private final double minerGasUnitInDollars;
    private final long minGasPriceTarget;
    private final int uncleListLimit;
    private final int uncleGenerationLimit;
    private final GasLimitConfig gasLimit;

    public MiningConfig(UscAddress coinbaseAddress, double minFeesNotifyInDollars, double minerGasUnitInDollars, long minGasPriceTarget, int uncleListLimit, int uncleGenerationLimit, GasLimitConfig gasLimit) {
        this.coinbaseAddress = coinbaseAddress;
        this.minFeesNotifyInDollars = minFeesNotifyInDollars;
        this.minerGasUnitInDollars = minerGasUnitInDollars;
        this.minGasPriceTarget= minGasPriceTarget;
        this.uncleListLimit = uncleListLimit;
        this.uncleGenerationLimit = uncleGenerationLimit;
        this.gasLimit = gasLimit;
    }

    public UscAddress getCoinbaseAddress() {
        return coinbaseAddress;
    }

    public double getMinFeesNotifyInDollars() {
        return minFeesNotifyInDollars;
    }

    public double getGasUnitInDollars() {
        return minerGasUnitInDollars;
    }

    public long getMinGasPriceTarget() {
        return minGasPriceTarget;
    }

    public int getUncleListLimit() {
        return uncleListLimit;
    }

    public int getUncleGenerationLimit() {
        return uncleGenerationLimit;
    }

    public GasLimitConfig getGasLimit() {
        return gasLimit;
    }
}
