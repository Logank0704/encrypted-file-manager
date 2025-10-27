package com.kc.encrypted.efm.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
public class StoredFile {
    @Id
    @GeneratedValue
    private UUID id;

    private String originalName;
    private long size;
    private String contentType;
    private String path;     // relative path of encrypted blob
    private String nonce;    // base64 IV
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    // ---- getters / setters ----
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
