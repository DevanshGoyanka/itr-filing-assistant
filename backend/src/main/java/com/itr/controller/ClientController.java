package com.itr.controller;

import com.itr.dto.ClientRequest;
import com.itr.dto.ClientResponse;
import com.itr.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<List<ClientResponse>> getClients(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(clientService.getClientsByUser(userId));
    }

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody ClientRequest request,
                                                        Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ClientResponse response = clientService.createClient(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable Long clientId,
                                                     Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(clientService.getClientById(clientId, userId));
    }

    @GetMapping("/{clientId}/years")
    public ResponseEntity<List<ClientResponse.YearStatus>> getClientYears(@PathVariable Long clientId,
                                                                           Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(clientService.getClientYears(clientId, userId));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<ClientResponse> updateClient(@PathVariable Long clientId,
                                                        @Valid @RequestBody ClientRequest request,
                                                        Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ClientResponse response = clientService.updateClient(clientId, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long clientId,
                                              Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        clientService.deleteClient(clientId, userId);
        return ResponseEntity.noContent().build();
    }
}
