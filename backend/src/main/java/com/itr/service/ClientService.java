package com.itr.service;

import com.itr.dto.ClientRequest;
import com.itr.dto.ClientResponse;
import com.itr.entity.Client;
import com.itr.entity.ClientYearData;
import com.itr.entity.User;
import com.itr.exception.ConflictException;
import com.itr.exception.ForbiddenException;
import com.itr.exception.ResourceNotFoundException;
import com.itr.repository.ClientRepository;
import com.itr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ClientResponse> getClientsByUser(Long userId) {
        return clientRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long clientId, Long userId) {
        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        return toResponse(client);
    }

    @Transactional(readOnly = true)
    public List<ClientResponse.YearStatus> getClientYears(Long clientId, Long userId) {
        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        return client.getYearData().stream()
                .map(yd -> ClientResponse.YearStatus.builder()
                        .year(yd.getAssessmentYear())
                        .status(yd.getStatus())
                        .itrType(yd.getItrType() != null ? yd.getItrType() : "ITR1")
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public ClientResponse createClient(ClientRequest request, Long userId) {
        if (clientRepository.existsByPanAndUserId(request.getPan(), userId)) {
            throw new ConflictException("PAN already registered: " + request.getPan());
        }

        // Validate DOB age between 18 and 100
        if (request.getDob() != null) {
            int age = Period.between(request.getDob(), LocalDate.now()).getYears();
            if (age < 18 || age > 100) {
                throw new IllegalArgumentException("Age must be between 18 and 100 years");
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Client client = Client.builder()
                .user(user)
                .pan(request.getPan())
                .name(request.getName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .aadhaar(request.getAadhaar())
                .dob(request.getDob())
                .build();

        client = clientRepository.save(client);
        return toResponse(client);
    }

    @Transactional
    public ClientResponse updateClient(Long clientId, ClientRequest request, Long userId) {
        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Validate DOB age between 18 and 100
        if (request.getDob() != null) {
            int age = Period.between(request.getDob(), LocalDate.now()).getYears();
            if (age < 18 || age > 100) {
                throw new IllegalArgumentException("Age must be between 18 and 100 years");
            }
        }

        // Check if PAN is being changed and if new PAN already exists
        if (!client.getPan().equals(request.getPan()) && 
            clientRepository.existsByPanAndUserId(request.getPan(), userId)) {
            throw new ConflictException("PAN already registered: " + request.getPan());
        }

        client.setPan(request.getPan());
        client.setName(request.getName());
        client.setEmail(request.getEmail());
        client.setMobile(request.getMobile());
        client.setAadhaar(request.getAadhaar());
        client.setDob(request.getDob());

        client = clientRepository.save(client);
        return toResponse(client);
    }

    @Transactional
    public void deleteClient(Long clientId, Long userId) {
        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        clientRepository.delete(client);
    }

    public Client getClientEntity(Long clientId, Long userId) {
        return clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
    }

    private ClientResponse toResponse(Client client) {
        List<ClientResponse.YearStatus> years = client.getYearData().stream()
                .map(yd -> ClientResponse.YearStatus.builder()
                        .year(yd.getAssessmentYear())
                        .status(yd.getStatus())
                        .itrType(yd.getItrType() != null ? yd.getItrType() : "ITR1")
                        .build())
                .collect(Collectors.toList());

        return ClientResponse.builder()
                .id(client.getId())
                .pan(client.getPan())
                .name(client.getName())
                .email(client.getEmail())
                .mobile(client.getMobile())
                .aadhaar(client.getAadhaar())
                .dob(client.getDob())
                .years(years)
                .build();
    }
}
