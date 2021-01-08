package com.airxiechao.axcboot.communication.rest.security;

import io.undertow.server.HttpServerExchange;

public interface AuthRoleChecker {
    boolean hasRole(HttpServerExchange exchange, AuthPrincipal principal, String[] roles);
}
