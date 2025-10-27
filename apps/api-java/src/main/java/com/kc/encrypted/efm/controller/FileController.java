package com.kc.encrypted.efm.controller;

import com.kc.encrypted.efm.domain.StoredFile;
import com.kc.encrypted.efm.service.StorageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/files")
public class FileController {

    private final StorageService storage;

    public FileController(StorageService storage) {
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws Exception {
        StoredFile saved = storage.saveEncrypted(file);
        return ResponseEntity.ok(Map.of(
            "id", saved.getId(),
            "name", saved.getOriginalName(),
            "size", saved.getSize(),
            "contentType", saved.getContentType(),
            "createdAt", saved.getCreatedAt()
        ));
    }

    @GetMapping
    public ResponseEntity<?> list() {
        List<StoredFile> items = storage.list();
        return ResponseEntity.ok(
            items.stream().map(sf -> Map.of(
                "id", sf.getId(),
                "name", sf.getOriginalName(),
                "size", sf.getSize(),
                "contentType", sf.getContentType(),
                "createdAt", sf.getCreatedAt()
            )).toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> download(@PathVariable UUID id) throws Exception {
        StorageService.DecryptedFile df = storage.readDecrypted(id);
        ByteArrayResource body = new ByteArrayResource(df.bytes());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(df.filename()).build());
        headers.set(HttpHeaders.CONTENT_TYPE, df.contentType());
        return ResponseEntity.ok().headers(headers).body(body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) throws Exception {
        storage.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> rename(@PathVariable UUID id, @RequestBody RenameRequest body) {
        StoredFile updated = storage.rename(id, body.name());
        return ResponseEntity.ok(Map.of(
            "id", updated.getId(),
            "name", updated.getOriginalName(),
            "size", updated.getSize(),
            "contentType", updated.getContentType(),
            "createdAt", updated.getCreatedAt()
        ));
    }

    public record RenameRequest(String name) {}
}
