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
package co.usc.rpc.modules;

import co.usc.jsonrpc.*;
import co.usc.rpc.modules.eth.subscribe.EthSubscribeRequest;
import co.usc.rpc.modules.eth.subscribe.EthUnsubscribeRequest;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.netty.channel.ChannelHandlerContext;

/**
 * This is the base class for USC supported JSON-RPC requests.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "method", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = EthSubscribeRequest.class, name = "eth_subscribe"),
        @JsonSubTypes.Type(value = EthUnsubscribeRequest.class, name = "eth_unsubscribe"),
})
public abstract class UscJsonRpcRequest extends JsonRpcRequest<UscJsonRpcMethod> {
    public UscJsonRpcRequest(
            JsonRpcVersion version,
            UscJsonRpcMethod method,
            int id) {
        super(version, method, id);
    }

    /**
     * Inheritors should implement this method by delegating to the corresponding visitor method.
     */
    public abstract JsonRpcResultOrError accept(UscJsonRpcRequestVisitor visitor, ChannelHandlerContext ctx);
}
