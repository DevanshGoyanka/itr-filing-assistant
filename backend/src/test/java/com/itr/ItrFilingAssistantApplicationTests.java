package com.itr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itr.dto.AuthRequest;
import com.itr.dto.AuthResponse;
import com.itr.dto.ClientRequest;
import com.itr.repository.ClientRepository;
import com.itr.repository.ClientYearDataRepository;
import com.itr.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ItrFilingAssistantApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientYearDataRepository yearDataRepository;

    private static String authToken;

    @BeforeEach
    void setup() {
        // Clean up before each test to avoid conflicts
    }

    @AfterAll
    static void cleanup() {
        authToken = null;
    }

    // ==================== AUTH TESTS ====================

    @Test
    @Order(1)
    void testRegister_Success() throws Exception {
        // Clean up
        yearDataRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();

        AuthRequest request = new AuthRequest();
        request.setEmail("test@itr.com");
        request.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("test@itr.com"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        authToken = response.getToken();
    }

    @Test
    @Order(2)
    void testRegister_DuplicateEmail() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@itr.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    @Order(3)
    void testRegister_InvalidEmail() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("invalid-email");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    void testRegister_ShortPassword() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("new@itr.com");
        request.setPassword("short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    void testLogin_Success() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@itr.com");
        request.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("test@itr.com"))
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        authToken = response.getToken();
    }

    @Test
    @Order(6)
    void testLogin_WrongPassword() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@itr.com");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== CLIENT TESTS ====================

    @Test
    @Order(10)
    void testCreateClient_Success() throws Exception {
        ClientRequest request = new ClientRequest();
        request.setPan("ABCDE1234F");
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setMobile("+919999999999");
        request.setDob(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pan").value("ABCDE1234F"))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    @Order(11)
    void testCreateClient_DuplicatePAN() throws Exception {
        ClientRequest request = new ClientRequest();
        request.setPan("ABCDE1234F");
        request.setName("Jane Doe");

        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(12)
    void testCreateClient_InvalidPAN() throws Exception {
        ClientRequest request = new ClientRequest();
        request.setPan("INVALID");
        request.setName("Test User");

        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(13)
    void testCreateClient_InvalidAadhaar() throws Exception {
        ClientRequest request = new ClientRequest();
        request.setPan("XYZPQ5678M");
        request.setName("Test User");
        request.setAadhaar("12345"); // too short

        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(14)
    void testGetClients_Success() throws Exception {
        mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].pan").value("ABCDE1234F"));
    }

    @Test
    @Order(15)
    void testGetClients_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(16)
    void testGetClientById_Success() throws Exception {
        // Get clients to find the ID
        MvcResult listResult = mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = listResult.getResponse().getContentAsString();
        Long clientId = objectMapper.readTree(jsonResponse).get(0).get("id").asLong();

        mockMvc.perform(get("/api/clients/" + clientId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pan").value("ABCDE1234F"))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    @Order(17)
    void testGetClientById_NotFound() throws Exception {
        mockMvc.perform(get("/api/clients/99999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    // ==================== PREFILL TESTS ====================

    @Test
    @Order(20)
    void testUploadPrefill_JsonPaste_Success() throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();

        Long clientId = objectMapper.readTree(
                listResult.getResponse().getContentAsString()).get(0).get("id").asLong();

        String prefillJson = """
            {
                "personalInfo": {
                    "name": "John Doe",
                    "pan": "ABCDE1234F",
                    "dob": "1990-01-01"
                },
                "basicDetails": {
                    "salary": 800000,
                    "otherSources": 50000,
                    "assessmentYear": "AY2025-26"
                },
                "deductions": {
                    "section80C": 150000,
                    "section80D": 25000
                }
            }
            """;

        mockMvc.perform(post("/api/clients/" + clientId + "/prefill/AY2025-26")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prefillJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.incomeSummary.grossSalary").value(800000.0))
                .andExpect(jsonPath("$.incomeSummary.section80C").value(150000.0))
                .andExpect(jsonPath("$.incomeSummary.section80D").value(25000.0));
    }

    @Test
    @Order(21)
    void testUploadPrefill_PanMismatch() throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();

        Long clientId = objectMapper.readTree(
                listResult.getResponse().getContentAsString()).get(0).get("id").asLong();

        String prefillJson = """
            {
                "personalInfo": {
                    "name": "John Doe",
                    "pan": "FGHIJ5678K",
                    "dob": "1990-01-01"
                },
                "basicDetails": {
                    "salary": 800000
                }
            }
            """;

        mockMvc.perform(post("/api/clients/" + clientId + "/prefill/AY2025-26")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prefillJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        "PAN mismatch: Client PAN is ABCDE1234F but prefill contains FGHIJ5678K"));
    }

    @Test
    @Order(22)
    void testUploadPrefill_InvalidJson() throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();

        Long clientId = objectMapper.readTree(
                listResult.getResponse().getContentAsString()).get(0).get("id").asLong();

        mockMvc.perform(post("/api/clients/" + clientId + "/prefill/AY2025-26")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(23)
    void testUploadPrefill_InvalidAssessmentYear() throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();

        Long clientId = objectMapper.readTree(
                listResult.getResponse().getContentAsString()).get(0).get("id").asLong();

        mockMvc.perform(post("/api/clients/" + clientId + "/prefill/2025-26")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(24)
    void testUploadPrefill_YearIsolation() throws Exception {
        // Upload for a different year
        MvcResult listResult = mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();

        Long clientId = objectMapper.readTree(
                listResult.getResponse().getContentAsString()).get(0).get("id").asLong();

        String prefillJson = """
            {
                "personalInfo": {
                    "name": "John Doe",
                    "pan": "ABCDE1234F"
                },
                "basicDetails": {
                    "salary": 1000000,
                    "assessmentYear": "AY2026-27"
                }
            }
            """;

        mockMvc.perform(post("/api/clients/" + clientId + "/prefill/AY2026-27")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prefillJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assessmentYear").value("AY2026-27"))
                .andExpect(jsonPath("$.incomeSummary.grossSalary").value(1000000.0));

        // Verify year isolation - AY2025-26 data should still have original values
        mockMvc.perform(get("/api/clients/" + clientId + "/prefill/AY2025-26")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentYear").value("AY2025-26"))
                .andExpect(jsonPath("$.incomeSummary.grossSalary").value(800000.0));
    }

    @Test
    @Order(25)
    void testGetClientYears() throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();

        Long clientId = objectMapper.readTree(
                listResult.getResponse().getContentAsString()).get(0).get("id").asLong();

        mockMvc.perform(get("/api/clients/" + clientId + "/years")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ==================== AUTHORIZATION TESTS ====================

    @Test
    @Order(30)
    void testCrossUserAccess() throws Exception {
        // Register a second user
        AuthRequest request = new AuthRequest();
        request.setEmail("user2@itr.com");
        request.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String user2Token = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class).getToken();

        // User2 should see empty clients
        mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        // User2 should not be able to access User1's client
        MvcResult listResult = mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();

        Long user1ClientId = objectMapper.readTree(
                listResult.getResponse().getContentAsString()).get(0).get("id").asLong();

        mockMvc.perform(get("/api/clients/" + user1ClientId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound());
    }
}
