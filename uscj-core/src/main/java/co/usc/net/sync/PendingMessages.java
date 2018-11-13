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
package co.usc.net.sync;

import co.usc.net.messages.MessageType;
import co.usc.net.messages.MessageWithId;
import com.google.common.annotations.VisibleForTesting;


import java.util.HashMap;
import java.util.Map;

public class PendingMessages {

    private Map<Long, MessageType> messages = new HashMap<>();
    private long lastRequestId;

    public void register(MessageWithId message) {
        this.messages.put(message.getId(), message.getResponseMessageType());
    }

    public long getNextRequestId(){
        return ++lastRequestId;
    }

    public boolean isPending(MessageWithId message) {
        long messageId = message.getId();
        if (!this.messages.containsKey(messageId) || this.messages.get(messageId) != message.getMessageType()) {
            return false;
        }

        this.messages.remove(messageId);

        return true;
    }

    public void clear() {
        this.messages.clear();
        // lastRequestId isn't cleaned on purpose
    }

    @VisibleForTesting
    public Map<Long, MessageType> getExpectedMessages() {
        return this.messages;
    }

    @VisibleForTesting
    public void registerExpectedMessage(MessageWithId message) {
        this.messages.put(message.getId(), message.getMessageType());
    }
}
