package io.moquette.broker.security;

import io.moquette.broker.ClientDescriptor;

public interface IConnectionFilter {
    boolean allowConnect(ClientDescriptor clientDescriptor);
}
