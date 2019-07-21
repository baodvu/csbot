package com.codesignal.csbot.adapters.codesignal.message;

import java.util.List;
import java.util.Map;

public class LoginMessage extends MethodMessage {
    public LoginMessage(String email, String passwordDigest, String algorithm) {
        super("login",
                List.of(
                        Map.of(
                                "user", Map.of("email", email),
                                "password", Map.of("digest", passwordDigest, "algorithm", algorithm)
                        )
                )
        );
    }
}
