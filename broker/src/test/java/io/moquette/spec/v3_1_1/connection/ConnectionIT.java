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

package io.moquette.spec.v3_1_1.connection;

import io.moquette.BrokerConstants;
import io.moquette.broker.Server;
import io.moquette.integration.IntegrationUtils;
import io.moquette.testclient.RawClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConnectionIT {

    Server m_server;

    @TempDir
    Path tempFolder;

    protected void startServer(String dbPath) throws IOException {
        final Properties configProps = IntegrationUtils.prepareTestProperties(dbPath);
        m_server = new Server();
        m_server.startServer(configProps);
    }

    @BeforeEach
    public void setUp() throws Exception {
        String dbPath = IntegrationUtils.tempH2Path(tempFolder);
        startServer(dbPath);
    }

    @AfterEach
    public void tearDown() {
        m_server.stopServer();

        File dbFile = new File(BrokerConstants.DEFAULT_PERSISTENT_PATH);
        if (dbFile.exists()) {
            assertTrue(dbFile.delete(),
                "Error deleting the moquette db file " + BrokerConstants.DEFAULT_PERSISTENT_PATH);
        }
        assertFalse(dbFile.exists());
    }

    @Test
    @Timeout(3)
    public void testConnectThenClose() throws Exception {
        RawClient.connect("127.0.0.1", 1883).isConnected()
                // CONNECT
                .write(0x10) // MQTT Control Packet type(1)
                .write(0x13) // Remaining Length
                .write(0x00, 0x04) // Protocol Name Length
                .write("MQTT") // Protocol Name
                .write(0x04) // The value of the Protocol Level field for the version 3.1.1 of the
                             // protocol is 4 (0x04)

                // Connect Flags
                // User Name Flag(0)
                // Password Flag(0)
                // Will Retain(0)
                // Will QoS(00)
                // Will Flag(0)
                // Clean Session(1)
                .write(0x02) // Reserved(0)
                .write(0x00, 0x00) // Keep Alive

                // Payload
                .write(0x00, 0x07) // Client Identifier Length
                .write("client1") // Client Identifier
                .flush()

                // CONNACK
                .read(0x20) // MQTT Control Packet type(2)
                .read(0x02) // Remaining Length

                // Connect Acknowledge Flags
                .read(0x00) // Session Present Flag(0)

                // Connect Return code
                .read(0x00) // Connection Accepted

                // DISCONNECT
                .write(0xE0) // MQTT Control Packet type(14)
                .write(0x00) // Remaining Length
                .closed(1000);
    }

    @Test
    @Timeout(3)
    public void testConnectWithInvalidWillQoS() throws Exception {
        RawClient.connect("127.0.0.1", 1883).isConnected()
                // CONNECT
                .write(0x10) // MQTT Control Packet type(1)
                .write(0x13) // Remaining Length
                .write(0x00, 0x04) // Protocol Name Length
                .write("MQTT") // Protocol Name
                .write(0x04) // The value of the Protocol Level field for the version 3.1.1 of the
                             // protocol is 4 (0x04)

                // Connect Flags
                // User Name Flag(0)
                // Password Flag(0)
                // Will Retain(0)
                // Will QoS(11) - It MUST NOT be 3 (0x03). Server should close the connection.
                // Will Flag(1)
                // Clean Session(1)
                .write(0x1E) // Reserved(0)
                .write(0x00, 0x00) // Keep Alive

                // Payload
                .write(0x00, 0x07) // Client Identifier Length
                .write("client1") // Client Identifier
                .flush()

                .closed(1000);
    }

    @Disabled("Need to validate the test case.")
    @Test
    @Timeout(15)
    public void testConnectWithWillFlagSetToZeroButWillQoSFlagSetToNonZero() throws Exception {
        RawClient.connect("127.0.0.1", 1883).isConnected()
                // CONNECT
                .write(0x10) // MQTT Control Packet type(1)
                .write(0x12) // Remaining Length
                .write(0x00, 0x04) // Protocol Name Length
                .write("MQTT") // Protocol Name
                .write(0x04) // The value of the Protocol Level field for the version 3.1.1 of the
                             // protocol is 4 (0x04)

                // Connect Flags
                // User Name Flag(0)
                // Password Flag(0)
                // Will Retain(0)
                // Will QoS(01) - If the Will Flag is set to 0, then the Will QoS MUST be set to 0
                // Will Flag(0)
                // Clean Session(1)
                .write(0x0A) // Reserved(0)

                .write(0x00, 0x00) // Keep Alive

                // Payload
                .write(0x00, 0x07) // Client Identifier Length
                .write("client1") // Client Identifier

                // Server MUST close the Network Connection due to invalid Will QoS flag
                .closed();
    }

}
