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

import io.moquette.BrokerConstants;
import io.moquette.api.Topic;
import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.broker.security.IAuthenticator;
import io.moquette.broker.security.IAuthorizatorPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigurationClassLoaderTest implements IAuthenticator, IAuthorizatorPolicy {

    Server m_server;
    IConfig m_config;

    @TempDir
    Path tempFolder;
    private String dbPath;

    protected void startServer(Properties props) throws IOException {
        m_server = new Server();
        m_config = new MemoryConfig(props);
        m_server.startServer(m_config);
    }

    @BeforeEach
    public void setUp() {
        dbPath = IntegrationUtils.tempH2Path(tempFolder);
    }

    @AfterEach
    public void tearDown() {
        m_server.stopServer();
    }

    @Test
    public void loadAuthenticator() throws Exception {
        Properties props = new Properties(IntegrationUtils.prepareTestProperties(dbPath));
        props.setProperty(BrokerConstants.AUTHENTICATOR_CLASS_NAME, getClass().getName());
        startServer(props);
        assertTrue(true);
    }

    @Test
    public void loadAuthorizator() throws Exception {
        Properties props = new Properties(IntegrationUtils.prepareTestProperties(dbPath));
        props.setProperty(BrokerConstants.AUTHORIZATOR_CLASS_NAME, getClass().getName());
        startServer(props);
        assertTrue(true);
    }

    @Override
    public boolean checkValid(String clientID, String username, byte[] password) {
        return true;
    }

    @Override
    public boolean canWrite(Topic topic, String user, String client) {
        return true;
    }

    @Override
    public boolean canRead(Topic topic, String user, String client) {
        return true;
    }

}
