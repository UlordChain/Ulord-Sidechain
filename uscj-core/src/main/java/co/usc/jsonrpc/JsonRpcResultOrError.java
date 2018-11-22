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
package co.usc.jsonrpc;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This is the base class for JSON-RPC results and errors.
 * It can generate a response with the correct result or error response type.
 */
public interface JsonRpcResultOrError {
    /**
     * @return a response according to the result or error state of this object.
     * @param messageId the message ID
     */
    @JsonIgnore
    JsonRpcIdentifiableMessage responseFor(int messageId);
}
