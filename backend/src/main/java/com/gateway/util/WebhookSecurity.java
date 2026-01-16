package com.gateway.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class WebhookSecurity {

    private static final String ALGORITHM = "HmacSHA256";

    public static String generateSignature(String payload, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance(ALGORITHM);
            SecretKeySpec secret_key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), 
                ALGORITHM
            );
            sha256_HMAC.init(secret_key);

            byte[] hash = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            
            // Convert byte array to Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to calculate HMAC signature", e);
        }
    }
}