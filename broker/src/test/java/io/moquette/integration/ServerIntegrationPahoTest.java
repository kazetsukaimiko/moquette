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

package io.moquette.integration;

import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class ServerIntegrationPahoTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServerIntegrationPahoTest.class);

    Server m_server;
    IMqttClient m_client;
    IMqttClient m_publisher;
    MessageCollector m_messagesCollector;
    IConfig m_config;

    @TempDir
    Path tempFolder;
    private String dbPath;

    protected void startServer(String dbPath) throws IOException {
        m_server = new Server();
        final Properties configProps = IntegrationUtils.prepareTestProperties(dbPath);
        m_config = new MemoryConfig(configProps);
        m_server.startServer(m_config);
    }

    @BeforeAll
    public static void beforeTests() {
        Awaitility.setDefaultTimeout(Durations.ONE_SECOND);
    }

    @BeforeEach
    public void setUp() throws Exception {
        dbPath = IntegrationUtils.tempH2Path(tempFolder);
        startServer(dbPath);

        MqttClientPersistence dataStore = new MqttDefaultFilePersistence(IntegrationUtils.newFolder(tempFolder, "client").getAbsolutePath());
        MqttClientPersistence pubDataStore = new MqttDefaultFilePersistence(IntegrationUtils.newFolder(tempFolder, "publisher").getAbsolutePath());

        m_client = new MqttClient("tcp://localhost:1883", "TestClient", dataStore);
        m_messagesCollector = new MessageCollector();
        m_client.setCallback(m_messagesCollector);

        m_publisher = new MqttClient("tcp://localhost:1883", "Publisher", pubDataStore);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (m_client != null && m_client.isConnected()) {
            m_client.disconnect();
        }

        if (m_publisher != null && m_publisher.isConnected()) {
            m_publisher.disconnect();
        }

        stopServer();
    }

    private void stopServer() {
        m_server.stopServer();
    }

    @Disabled("This test hasn't any meaning using in memory storage service")
    @Test
    public void testCleanSession_maintainClientSubscriptions_withServerRestart() throws Exception {
        LOG.info("*** testCleanSession_maintainClientSubscriptions_withServerRestart ***");
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
        m_client.connect(options);
        m_client.subscribe("/topic", 0);
        m_client.disconnect();

        m_server.stopServer();

        m_server.startServer(IntegrationUtils.prepareTestProperties(dbPath));

        // reconnect and publish
        m_client.connect(options);
        m_client.publish("/topic", "Test my payload".getBytes(UTF_8), 0, false);

        Awaitility.await().until(m_messagesCollector::isMessageReceived);
        assertEquals("/topic", m_messagesCollector.retrieveTopic());
    }

    /**
     * subscriber A connect and subscribe on "a/b" QoS 1 subscriber B connect and subscribe on "a/+"
     * BUT with QoS 2 publisher connects and send a message "hello" on "a/b" subscriber A must
     * receive a notification with QoS1 subscriber B must receive a notification with QoS2
     */
    @Test
    public void checkSubscribersGetCorrectQosNotifications() throws Exception {
        LOG.info("*** checkSubscribersGetCorrectQosNotifications ***");

        MqttClientPersistence dsSubscriberA = new MqttDefaultFilePersistence(IntegrationUtils.newFolder(tempFolder, "subscriberA").getAbsolutePath());

        MqttClient subscriberA = new MqttClient("tcp://localhost:1883", "SubscriberA", dsSubscriberA);
        MessageCollector cbSubscriberA = new MessageCollector();
        subscriberA.setCallback(cbSubscriberA);
        subscriberA.connect();
        subscriberA.subscribe("a/b", 1);

        MqttClientPersistence dsSubscriberB = new MqttDefaultFilePersistence(IntegrationUtils.newFolder(tempFolder, "subscriberB").getAbsolutePath());

        MqttClient subscriberB = new MqttClient("tcp://localhost:1883", "SubscriberB", dsSubscriberB);
        MessageCollector cbSubscriberB = new MessageCollector();
        subscriberB.setCallback(cbSubscriberB);
        subscriberB.connect();
        subscriberB.subscribe("a/+", 2);

        m_client.connect();
        m_client.publish("a/b", "Hello world MQTT!!".getBytes(UTF_8), 2, false);

        Awaitility.await().until(cbSubscriberA::isMessageReceived);
        MqttMessage messageOnA = cbSubscriberA.retrieveMessage();
        assertEquals("Hello world MQTT!!", new String(messageOnA.getPayload(), UTF_8));
        assertEquals(1, messageOnA.getQos());
        subscriberA.disconnect();

        Awaitility.await().until(cbSubscriberB::isMessageReceived);
        MqttMessage messageOnB = cbSubscriberB.retrieveMessage();
        assertNotNull(messageOnB, "MUST be a received message");
        assertEquals("Hello world MQTT!!", new String(messageOnB.getPayload(), UTF_8));
        assertEquals(2, messageOnB.getQos());
        subscriberB.disconnect();
    }

    @Test
    public void testSubcriptionDoesntStayActiveAfterARestart() throws Exception {
        // clientForSubscribe1 connect and subscribe to /topic QoS2
        MqttClientPersistence dsSubscriberA = new MqttDefaultFilePersistence(
            IntegrationUtils.newFolder(tempFolder, "clientForSubscribe1").getAbsolutePath());

        MqttClient clientForSubscribe1 = new MqttClient("tcp://localhost:1883", "clientForSubscribe1", dsSubscriberA);
        MessageCollector cbSubscriber1 = new MessageCollector();
        clientForSubscribe1.setCallback(cbSubscriber1);
        clientForSubscribe1.connect();
        clientForSubscribe1.subscribe("topic", 0);

        // integration stop
        m_server.stopServer();
        System.out.println("\n\n SEVER REBOOTING \n\n");
        // integration start
        startServer(dbPath);

        // clientForSubscribe2 connect and subscribe to /topic QoS2
        MqttClientPersistence dsSubscriberB = new MqttDefaultFilePersistence(
            IntegrationUtils.newFolder(tempFolder, "clientForSubscribe2").getAbsolutePath());
        MqttClient clientForSubscribe2 = new MqttClient("tcp://localhost:1883", "clientForSubscribe2", dsSubscriberB);
        MessageCollector cbSubscriber2 = new MessageCollector();
        clientForSubscribe2.setCallback(cbSubscriber2);
        clientForSubscribe2.connect();
        clientForSubscribe2.subscribe("topic", 0);

        // clientForPublish publish on /topic with QoS2 a message
        MqttClientPersistence dsSubscriberPUB = new MqttDefaultFilePersistence(
            IntegrationUtils.newFolder(tempFolder, "clientForPublish").getAbsolutePath());
        MqttClient clientForPublish = new MqttClient("tcp://localhost:1883", "clientForPublish", dsSubscriberPUB);
        clientForPublish.connect();
        clientForPublish.publish("topic", "Hello".getBytes(UTF_8), 2, true);

        // verify clientForSubscribe1 doesn't receive a notification but clientForSubscribe2 yes
        LOG.info("Before waiting to receive 1 sec from {}", clientForSubscribe1.getClientId());
        assertFalse(clientForSubscribe1.isConnected());
        assertTrue(clientForSubscribe2.isConnected());
        LOG.info("Waiting to receive 1 sec from {}", clientForSubscribe2.getClientId());
        Awaitility.await()
            .atMost(1, TimeUnit.SECONDS)
            .until(cbSubscriber2::isMessageReceived);
        assertEquals("Hello", new String(cbSubscriber2.retrieveMessage().getPayload(), UTF_8));
    }
}
