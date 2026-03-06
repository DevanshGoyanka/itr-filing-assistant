package com.itr.controller;

import com.itr.dto.Itr1FormData;
import com.itr.service.Itr1FormService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients/{clientId}/itr/{year}")
@RequiredArgsConstructor
public class Itr1Controller {

    private final Itr1FormService itr1FormService;

    /**
     * GET — Retrieve current ITR-1 form data (auto-built from prefill if no saved data).
     */
    @GetMapping
    public ResponseEntity<Itr1FormData> getFormData(
            @PathVariable Long clientId,
            @PathVariable String year,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Itr1FormData formData = itr1FormService.getFormData(clientId, year, userId);
        return ResponseEntity.ok(formData);
    }

    /**
     * PUT — Save user edits to the form.
     */
    @PutMapping
    public ResponseEntity<Itr1FormData> saveFormData(
            @PathVariable Long clientId,
            @PathVariable String year,
            @RequestBody Itr1FormData formData,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Itr1FormData saved = itr1FormService.saveFormData(clientId, year, formData, userId);
        return ResponseEntity.ok(saved);
    }

    /**
     * POST /compute — Run full computation (salary, HP, OS, deductions, tax).
     */
    @PostMapping("/compute")
    public ResponseEntity<Itr1FormData> computeForm(
            @PathVariable Long clientId,
            @PathVariable String year,
            @RequestBody Itr1FormData formData,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Itr1FormData computed = itr1FormService.computeForm(clientId, year, formData, userId);
        return ResponseEntity.ok(computed);
    }

    /**
     * GET /download — Download finalized ITR-1 JSON.
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadItr1Json(
            @PathVariable Long clientId,
            @PathVariable String year,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String json = itr1FormService.generateItr1Json(clientId, year, userId);

        String filename = "ITR1_" + year + ".json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.getBytes());
    }
}
