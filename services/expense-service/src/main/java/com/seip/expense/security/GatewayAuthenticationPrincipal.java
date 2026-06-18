package com.seip.expense.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GatewayAuthenticationPrincipal {

    private final Long userId;
    private final String email;
    private final String roles;

    @Override
    public String toString() {
        return "GatewayPrincipal{userId=" + userId + ", email='" + email + "'}";
    }
}
