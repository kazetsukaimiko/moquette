/*
 * Copyright (c) 2012-2018 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.moquette.broker;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Some useful assertions used by Netty's EmbeddedChannel in tests.
 */
public final class NettyChannelAssertions {

    public static void assertEqualsConnAck(MqttConnectReturnCode expectedCode, Object connAck) {
        assertEqualsConnAck(null, expectedCode, connAck);
    }

    public static void assertEqualsConnAck(String msg, MqttConnectReturnCode expectedCode, Object connAck) {
        assertTrue(connAck instanceof MqttConnAckMessage, "connAck is not an instance of ConnAckMessage");
        MqttConnAckMessage connAckMsg = (MqttConnAckMessage) connAck;

        if (msg == null)
            assertEquals(expectedCode, connAckMsg.variableHeader().connectReturnCode());
        else
            assertEquals(expectedCode, connAckMsg.variableHeader().connectReturnCode(), msg);
    }

    public static void assertConnAckAccepted(EmbeddedChannel channel) {
        channel.flush();
        assertEqualsConnAck(CONNECTION_ACCEPTED, channel.readOutbound());
    }

    public static void assertEqualsSubAck(/* byte expectedCode, */ Object subAck) {
        assertTrue(subAck instanceof MqttSubAckMessage);
        // SubAckMessage connAckMsg = (SubAckMessage) connAck;
        // assertEquals(expectedCode, connAckMsg.getReturnCode());
    }

    private NettyChannelAssertions() {
    }
}
