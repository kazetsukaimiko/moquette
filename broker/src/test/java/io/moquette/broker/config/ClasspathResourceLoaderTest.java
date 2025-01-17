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

package io.moquette.broker.config;

import io.moquette.BrokerConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClasspathResourceLoaderTest {

    @Test
    public void testSetProperties() {
        IResourceLoader classpathLoader = new ClasspathResourceLoader();
        final IConfig classPathConfig = new ResourceLoaderConfig(classpathLoader);
        assertEquals("" + BrokerConstants.PORT, classPathConfig.getProperty(BrokerConstants.PORT_PROPERTY_NAME));
        classPathConfig.setProperty(BrokerConstants.PORT_PROPERTY_NAME, "9999");
        assertEquals("9999", classPathConfig.getProperty(BrokerConstants.PORT_PROPERTY_NAME));
    }

}
