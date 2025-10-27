package com.kc.encrypted.efm.service;

import com.kc.encrypted.efm.config.AppConfig;
import com.kc.encrypted.efm.util.KeyUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;   // 16-byte tag
    private static final int GCM_IV_BYTES = 12;    // 12-byte nonce

    private final AppConfig config;
    private final SecureRandom rng = new SecureRandom();
    private SecretKey key;

    // Explicit constructor (no Lombok)
    public CryptoService(AppConfig config) {
        this.config = config;
    }

    @PostConstruct
    void init() {
        String mk = config.masterKey();
        if (mk == null || mk.isBlank()) {
            throw new IllegalStateException(
                "Master key not provided. Set APP_MASTER_KEY env or app.master-key property to a 256-bit key (hex or base64).");
        }
        byte[] keyBytes = KeyUtils.parse256BitKey(mk);
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public byte[] newIv() {
        byte[] iv = new byte[GCM_IV_BYTES];
        rng.nextBytes(iv);
        return iv;
    }

    public void encrypt(InputStream in, OutputStream out, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        try (CipherOutputStream cos = new CipherOutputStream(out, cipher)) {
            in.transferTo(cos);
        }
    }

    public void decrypt(InputStream in, OutputStream out, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        try (CipherInputStream cis = new CipherInputStream(in, cipher)) {
            cis.transferTo(out);
        }
    }

    public String ivToBase64(byte[] iv) {
        return Base64.getEncoder().encodeToString(iv);
    }

    public byte[] ivFromBase64(String s) {
        return Base64.getDecoder().decode(s);
    }
}