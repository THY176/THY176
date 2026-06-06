package com.example.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long expireSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${auth.jwt-secret}") String secret,
            @Value("${auth.jwt-expire-seconds}") long expireSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.expireSeconds = expireSeconds;
    }

    public String createToken(AuthUser user) {
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", String.valueOf(user.getId()));
            payload.put("id", user.getId());
            payload.put("role", user.getRole());
            payload.put("name", user.getName());
            payload.put("iat", Instant.now().getEpochSecond());
            payload.put("exp", Instant.now().getEpochSecond() + expireSeconds);

            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsBytes(payload));
            String unsignedToken = encodedHeader + "." + encodedPayload;
            return unsignedToken + "." + sign(unsignedToken);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create JWT", e);
        }
    }

    public AuthUser verifyToken(String authorization) {
        try {
            if (authorization == null || authorization.isBlank()) {
                return null;
            }

            String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String unsignedToken = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
                return null;
            }

            Map<String, Object> payload = objectMapper.readValue(base64UrlDecode(parts[1]), MAP_TYPE);
            long expiresAt = toLong(payload.get("exp"));
            if (expiresAt <= Instant.now().getEpochSecond()) {
                return null;
            }

            Integer id = toInteger(payload.get("id"));
            String role = (String) payload.get("role");
            String name = (String) payload.get("name");
            if (id == null || role == null || role.isBlank()) {
                return null;
            }

            return new AuthUser(id, role, name);
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        return base64UrlEncode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }
}
