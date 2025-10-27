package com.kc.encrypted.efm.util;

import java.util.Base64;

public final class KeyUtils {
    private KeyUtils() {}

    /**
     * Accepts either 32-byte (256-bit) key in hex or base64.
     * Returns the raw 32 bytes. Throws if invalid.
     */
    public static byte[] parse256BitKey(String raw) {
        byte[] key = tryBase64(raw);
        if (key == null) key = tryHex(raw);
        if (key == null || key.length != 32) {
            throw new IllegalArgumentException("Master key must be 32 bytes (256-bit) in base64 or hex.");
        }
        return key;
    }

    private static byte[] tryBase64(String s) {
        try {
            byte[] decoded = Base64.getDecoder().decode(s);
            return decoded.length == 32 ? decoded : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static byte[] tryHex(String s) {
        String t = s.trim();
        if (t.startsWith("0x") || t.startsWith("0X")) t = t.substring(2);
        if (t.length() != 64) return null;
        byte[] out = new byte[32];
        for (int i = 0; i < 32; i++) {
            int hi = fromHex(t.charAt(2*i));
            int lo = fromHex(t.charAt(2*i+1));
            if (hi < 0 || lo < 0) return null;
            out[i] = (byte)((hi << 4) | lo);
        }
        return out;
        }

    private static int fromHex(char c) {
        if ('0' <= c && c <= '9') return c - '0';
        if ('a' <= c && c <= 'f') return 10 + (c - 'a');
        if ('A' <= c && c <= 'F') return 10 + (c - 'A');
        return -1;
    }
}
