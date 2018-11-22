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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The basic JSON-RPC request or response. It is required to have an ID.
 *
 * Note that the JSON-RPC 2.0 spec allows using strings as IDs, but our implementation doesn't.
 */
public abstract class JsonRpcIdentifiableMessage extends JsonRpcMessage {
    private final int id;

    public JsonRpcIdentifiableMessage(JsonRpcVersion version, int id) {
        super(version);
        this.id = requireNonNegative(id);
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public int getId() {
        return id;
    }

    private static int requireNonNegative(int id) {
        if (id < 0) {
            throw new IllegalArgumentException(
                    String.format("JSON-RPC message id should be a positive number, but was %s.", id)
            );
        }

        return id;
    }
}
