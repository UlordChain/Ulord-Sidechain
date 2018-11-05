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
package co.usc.rpc.netty;

import co.usc.jsonrpc.*;
import co.usc.rpc.EthSubscriptionNotificationEmitter;
import co.usc.rpc.JsonRpcSerializer;
import co.usc.rpc.modules.UscJsonRpcRequest;
import co.usc.rpc.modules.UscJsonRpcRequestVisitor;
import co.usc.rpc.modules.eth.subscribe.EthSubscribeRequest;
import co.usc.rpc.modules.eth.subscribe.EthSubscribeTypes;
import co.usc.rpc.modules.eth.subscribe.EthUnsubscribeRequest;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * This handler decodes inbound messages and dispatches valid JSON-RPC requests.
 *
 * Note that we split JSON-RPC handling in two because jsonrpc4j wasn't able to handle the PUB-SUB model.
 * Eventually, we might want to implement all methods in this style and remove jsonrpc4j.
 */
public class UscJsonRpcHandler
        extends SimpleChannelInboundHandler<ByteBufHolder>
        implements UscJsonRpcRequestVisitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(UscJsonRpcHandler.class);

    private final EthSubscriptionNotificationEmitter emitter;
    private final JsonRpcSerializer serializer;

    public UscJsonRpcHandler(EthSubscriptionNotificationEmitter emitter, JsonRpcSerializer serializer) {
        this.emitter = emitter;
        this.serializer = serializer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBufHolder msg) {
        try {
            UscJsonRpcRequest request = serializer.deserializeRequest(
                    new ByteBufInputStream(msg.copy().content())
            );

            // TODO(mc) we should support the ModuleDescription method filters
            JsonRpcResultOrError resultOrError = request.accept(this, ctx);
            JsonRpcIdentifiableMessage response = resultOrError.responseFor(request.getId());
            ctx.writeAndFlush(new TextWebSocketFrame(serializer.serializeMessage(response)));
            return;
        } catch (IOException e) {
            LOGGER.trace("Not a known or valid JsonRpcRequest", e);
        }

        // delegate to the next handler if the message can't be matched to a known JSON-RPC request
        ctx.fireChannelRead(msg.retain());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        emitter.unsubscribe(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public JsonRpcResultOrError visit(EthUnsubscribeRequest request, ChannelHandlerContext ctx) {
        boolean unsubscribed = emitter.unsubscribe(request.getParams().getSubscriptionId());
        return new JsonRpcBooleanResult(unsubscribed);
    }

    @Override
    public JsonRpcResultOrError visit(EthSubscribeRequest request, ChannelHandlerContext ctx) {
        EthSubscribeTypes subscribeType = request.getParams().getSubscription();
        switch (subscribeType) {
            case NEW_HEADS:
                return emitter.subscribe(ctx.channel());
            default:
                LOGGER.error("Subscription type {} is not implemented", subscribeType);
                return new JsonRpcInternalError();
        }
    }
}
