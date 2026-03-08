package com.itr.controller;

import com.itr.dto.PrefillResponse;
import com.itr.service.PrefillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/clients/{clientId}/prefill")
@RequiredArgsConstructor
public class PrefillController {

    private final PrefillService prefillService;

    @PostMapping(value = "/{year}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PrefillResponse> uploadPrefillFile(
            @PathVariable Long clientId,
            @PathVariable String year,
            @RequestParam("file") MultipartFile file,
            Authentication auth) throws IOException {

        Long userId = (Long) auth.getPrincipal();
        String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        PrefillResponse response = prefillService.uploadPrefill(clientId, year, jsonContent, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/{year}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PrefillResponse> uploadPrefillJson(
            @PathVariable Long clientId,
            @PathVariable String year,
            @RequestBody String jsonContent,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        PrefillResponse response = prefillService.uploadPrefill(clientId, year, jsonContent, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{year}")
    public ResponseEntity<PrefillResponse> getPrefillData(
            @PathVariable Long clientId,
            @PathVariable String year,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        PrefillResponse response = prefillService.getPrefillData(clientId, year, userId);
        return ResponseEntity.ok(response);
    }
}
