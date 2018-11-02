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
package co.usc.rpc;

import co.usc.jsonrpc.JsonRpcMessage;
import co.usc.rpc.modules.UscJsonRpcRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * This implements basic JSON-RPC serialization using Jackson.
 */
public class JacksonBasedRpcSerializer implements JsonRpcSerializer {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String serializeMessage(JsonRpcMessage message) throws JsonProcessingException {
        return mapper.writeValueAsString(message);
    }

    @Override
    public UscJsonRpcRequest deserializeRequest(InputStream source) throws IOException {
        return mapper.readValue(source, UscJsonRpcRequest.class);
    }
}
