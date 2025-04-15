package com.gk.kannel.utils.common;

import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class CommonUtils {

    private static final String CHARSET = "UTF-8";
    public static String encodeToSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString(); // Always 64-character length
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error encoding SHA-256", e);
        }
    }


    // Method to encode a URL
    public static String encodeURL(String input) {
        try {
            return URLEncoder.encode(input, CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error encoding URL", e);
        }
    }

    // Method to decode a URL
    public static String decodeURL(String input) {
        try {
            return URLDecoder.decode(input, CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error decoding URL", e);
        }
    }
}
