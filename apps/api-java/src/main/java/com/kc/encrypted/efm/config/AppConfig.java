package com.kc.encrypted.efm.config; 

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${file.storage.root:./data}")
    private String storageRoot;

    // Prefer ENV if present, else property `app.master-key`
    @Value("${APP_MASTER_KEY:#{null}}")
    private String masterKeyEnv;

    @Value("${app.master-key:#{null}}")
    private String masterKeyProp;

    public String storageRoot() {
        return storageRoot;
    }

    public String masterKey() {
        return masterKeyEnv != null && !masterKeyEnv.isBlank() ? masterKeyEnv : masterKeyProp;
    }
}