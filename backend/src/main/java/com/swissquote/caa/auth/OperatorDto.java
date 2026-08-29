package com.swissquote.caa.auth;

import java.util.UUID;

public record OperatorDto(UUID id, String username, String displayName, String role) {

    public static OperatorDto of(Operator operator) {
        return new OperatorDto(operator.getId(), operator.getUsername(),
            operator.getDisplayName(), operator.getRole());
    }
}
