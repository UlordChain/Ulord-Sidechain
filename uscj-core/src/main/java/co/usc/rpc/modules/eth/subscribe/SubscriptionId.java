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

import co.usc.jsonrpc.JsonRpcResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.ethereum.rpc.TypeConverter;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * The subscription id DTO for JSON serialization purposes.
 */
public class SubscriptionId extends JsonRpcResult {
    private final byte[] id;

    @JsonCreator
    public SubscriptionId(String hexId) {
        this.id = TypeConverter.stringHexToByteArray(hexId);
    }

    public SubscriptionId() {
        this.id = new byte[16];
        new SecureRandom().nextBytes(id);
    }

    public byte[] getId() {
        return Arrays.copyOf(id, id.length);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(id);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof SubscriptionId)) {
            return false;
        }

        SubscriptionId other = (SubscriptionId) o;
        return Arrays.equals(this.id, other.id);
    }

    @JsonValue
    @SuppressWarnings("unused")
    private String serialize() {
        return TypeConverter.toJsonHex(id);
    }
}
