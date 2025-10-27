package com.kc.encrypted.efm.service;

import com.kc.encrypted.efm.config.AppConfig;
import com.kc.encrypted.efm.domain.StoredFile;
import com.kc.encrypted.efm.repository.StoredFileRepository;
import com.kc.encrypted.efm.util.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final StoredFileRepository repo;
    private final CryptoService crypto;
    private final AppConfig config;

    private final S3Client s3;         // nullable when S3 disabled
    private final boolean s3Enabled;
    private final String s3Bucket;

    public StorageService(StoredFileRepository repo,
                          CryptoService crypto,
                          AppConfig config,
                          ObjectProvider<S3Client> s3Provider,
                          @Value("${s3.bucket:efm-blobs}") String s3Bucket) {
        this.repo = repo;
        this.crypto = crypto;
        this.config = config;

        this.s3 = s3Provider.getIfAvailable();
        this.s3Enabled = (this.s3 != null);
        this.s3Bucket = s3Bucket;

        log.info("Storage mode: {}", s3Enabled ? ("S3 bucket=" + this.s3Bucket)
                                              : ("LOCAL DISK (" + config.storageRoot() + ")"));
    }

    /** Save the uploaded file encrypted-at-rest; returns the metadata row. */
    public StoredFile saveEncrypted(MultipartFile mf) throws Exception {
        Objects.requireNonNull(mf, "file must not be null");
        if (mf.isEmpty()) throw new IllegalArgumentException("file is empty");

        UUID id = UUID.randomUUID();
        String objectKey = id.toString() + ".bin";
        byte[] iv = crypto.newIv();

        if (s3Enabled) {
            Path tmp = Files.createTempFile("efm-", ".enc");
            try (InputStream in = mf.getInputStream();
                 OutputStream out = Files.newOutputStream(tmp)) {
                crypto.encrypt(in, out, iv);
            }
            long encSize = Files.size(tmp);

            s3.putObject(
                PutObjectRequest.builder().bucket(s3Bucket).key(objectKey).build(),
                RequestBody.fromFile(tmp)
            );
            Files.deleteIfExists(tmp);
            log.debug("Saved encrypted object to S3: {}/{} ({} bytes)", s3Bucket, objectKey, encSize);
        } else {
            Path root = Path.of(config.storageRoot()).toAbsolutePath().normalize();
            IoUtils.ensureDir(root);
            Path outPath = root.resolve(objectKey);
            try (InputStream in = mf.getInputStream();
                 OutputStream out = Files.newOutputStream(outPath)) {
                crypto.encrypt(in, out, iv);
            }
            long encSize = Files.size(outPath);
            log.debug("Saved encrypted blob to disk: {} ({} bytes)", outPath, encSize);
        }

        StoredFile sf = new StoredFile();
        sf.setId(id);
        sf.setOriginalName(mf.getOriginalFilename() != null ? mf.getOriginalFilename() : "file");
        sf.setSize(mf.getSize());
        sf.setContentType(mf.getContentType() != null ? mf.getContentType() : "application/octet-stream");
        sf.setPath(objectKey);              // S3 key or local filename
        sf.setNonce(crypto.ivToBase64(iv)); // store IV for AES-GCM
        sf.setCreatedAt(Instant.now());

        return repo.save(sf);
    }

    /** List files, newest first. */
    public List<StoredFile> list() {
        List<StoredFile> items = repo.findAll();
        items.sort(Comparator.comparing(
            StoredFile::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return items;
    }

    /** Read & decrypt by id; returns bytes + filename + contentType. */
    public DecryptedFile readDecrypted(UUID id) throws Exception {
        StoredFile sf = repo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("File not found: " + id));

        byte[] iv = crypto.ivFromBase64(sf.getNonce());
        ByteArrayOutputStream baos =
            new ByteArrayOutputStream((int) Math.min(sf.getSize(), Integer.MAX_VALUE));

        if (s3Enabled) {
            try (InputStream obj = s3.getObject(
                    GetObjectRequest.builder().bucket(s3Bucket).key(sf.getPath()).build())) {
                crypto.decrypt(obj, baos, iv);
            }
        } else {
            Path inPath = Path.of(config.storageRoot()).toAbsolutePath().normalize().resolve(sf.getPath());
            if (!Files.exists(inPath)) {
                throw new NoSuchElementException("Encrypted blob missing on disk for id " + id + " at " + inPath);
            }
            try (InputStream in = Files.newInputStream(inPath)) {
                crypto.decrypt(in, baos, iv);
            }
        }

        return new DecryptedFile(sf.getOriginalName(), sf.getContentType(), baos.toByteArray());
    }

    /** Delete metadata + encrypted blob. */
    public void delete(UUID id) throws Exception {
        StoredFile sf = repo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("File not found: " + id));

        if (s3Enabled) {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(sf.getPath())
                    .build());
            log.debug("Deleted S3 object: {}/{}", s3Bucket, sf.getPath());
        } else {
            Path p = Path.of(config.storageRoot()).toAbsolutePath().normalize().resolve(sf.getPath());
            Files.deleteIfExists(p);
            log.debug("Deleted local blob: {}", p);
        }

        repo.delete(sf);
    }

    /** Rename (updates only metadata.originalName). */
    public StoredFile rename(UUID id, String newName) {
        StoredFile sf = repo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("File not found: " + id));
        sf.setOriginalName(newName);
        return repo.save(sf);
    }

    /** DTO for controller download responses. */
    public record DecryptedFile(String filename, String contentType, byte[] bytes) {}
}
