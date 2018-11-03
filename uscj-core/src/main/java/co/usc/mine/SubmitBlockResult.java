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

package co.usc.mine;

/**
 * Wraps the result of a MinerServer.SubmitUlordBlock() method call.
 *
 * @author martin.medina
 */
public class SubmitBlockResult {

    private final String status;

    private final String message;

    private final SubmittedBlockInfo blockInfo;

    public SubmitBlockResult(String status, String message) {
        this.status = status;
        this.message = message;
        this.blockInfo = null;
    }

    SubmitBlockResult(String status, String message, SubmittedBlockInfo blockInfo) {
        this.status = status;
        this.message = message;
        this.blockInfo = blockInfo;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public SubmittedBlockInfo getBlockInfo() {
        return blockInfo;
    }
}
