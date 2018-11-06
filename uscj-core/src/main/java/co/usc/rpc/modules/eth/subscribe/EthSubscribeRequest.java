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
package co.usc.rpc.modules.eth.subscribe;

import co.usc.jsonrpc.JsonRpcResultOrError;
import co.usc.jsonrpc.JsonRpcVersion;
import co.usc.rpc.modules.UscJsonRpcMethod;
import co.usc.rpc.modules.UscJsonRpcRequest;
import co.usc.rpc.modules.UscJsonRpcRequestVisitor;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.channel.ChannelHandlerContext;

import java.util.Objects;

public class EthSubscribeRequest extends UscJsonRpcRequest {

    private final EthSubscribeParams params;

    @JsonCreator
    public EthSubscribeRequest(
            @JsonProperty("jsonrpc") JsonRpcVersion version,
            @JsonProperty("method") UscJsonRpcMethod method,
            @JsonProperty("id") Integer id,
            @JsonProperty("params") EthSubscribeParams params) {
        super(version, verifyMethod(method), Objects.requireNonNull(id));
        this.params = Objects.requireNonNull(params);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public EthSubscribeParams getParams() {
        return params;
    }

    @Override
    public JsonRpcResultOrError accept(UscJsonRpcRequestVisitor visitor, ChannelHandlerContext ctx) {
        return visitor.visit(this, ctx);
    }

    private static UscJsonRpcMethod verifyMethod(UscJsonRpcMethod method) {
        if (method != UscJsonRpcMethod.ETH_SUBSCRIBE) {
            throw new IllegalArgumentException(
                    "Wrong method mapped to eth_subscribe. Check JSON mapping configuration in JsonRpcRequest."
            );
        }

        return method;
    }
}
