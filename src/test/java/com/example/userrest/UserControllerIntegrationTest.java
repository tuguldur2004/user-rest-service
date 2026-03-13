package com.example.userrest;

import com.example.userrest.dto.TokenValidationResult;
import com.example.userrest.model.UserProfile;
import com.example.userrest.repository.UserProfileRepository;
import com.example.userrest.service.SoapAuthClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "soap.service.url=http://localhost:9999/ws",
        "spring.datasource.url=jdbc:h2:mem:testdb2;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileRepository repository;

    @MockBean
    private SoapAuthClient soapAuthClient;

    @BeforeEach
    void setup() {
        repository.deleteAll();
    }

    @Test
    void createProfile_withoutToken_shouldReturnUnauthorized() throws Exception {
        String body = """
                {
                                                                        \"authUserId\": 101,
                  \"username\": \"bat\",
                  \"email\": \"bat@mail.com\",
                  \"name\": \"Bat\",
                  \"bio\": \"hello\",
                  \"phone\": \"99887766\"
                }
                """;

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProfile_withValidToken_shouldReturnCreated() throws Exception {
        TokenValidationResult tokenResult = new TokenValidationResult();
        tokenResult.setValid(true);
        tokenResult.setUserId(101);
        tokenResult.setUsername("bat");
        tokenResult.setRole("USER");

        when(soapAuthClient.validateToken(anyString())).thenReturn(tokenResult);

        String body = """
                {
                  \"authUserId\": 101,
                  \"username\": \"bat\",
                  \"email\": \"bat@mail.com\",
                  \"name\": \"Bat\",
                  \"bio\": \"hello\",
                  \"phone\": \"99887766\"
                }
                """;

        mockMvc.perform(post("/users")
                .header("Authorization", "Bearer sample.jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("bat"));
    }

    @Test
    void getProfile_withoutToken_shouldReturnUnauthorized() throws Exception {
        UserProfile p = new UserProfile();
        p.setAuthUserId(101);
        p.setUsername("user1");
        p.setEmail("u1@mail.com");
        p.setName("User One");
        p = repository.save(p);

        mockMvc.perform(get("/users/" + p.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_withValidToken_shouldReturnOk() throws Exception {
        UserProfile p = new UserProfile();
        p.setAuthUserId(101);
        p.setUsername("user1");
        p.setEmail("u1@mail.com");
        p.setName("User One");
        p = repository.save(p);

        TokenValidationResult tokenResult = new TokenValidationResult();
        tokenResult.setValid(true);
        tokenResult.setUserId(101);
        tokenResult.setUsername("user1");
        tokenResult.setRole("USER");

        when(soapAuthClient.validateToken(anyString())).thenReturn(tokenResult);

        mockMvc.perform(get("/users/" + p.getId())
                .header("Authorization", "Bearer sample.jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("user1"));
    }

    @Test
    void updateProfile_notOwner_shouldReturnForbidden() throws Exception {
        UserProfile p = new UserProfile();
        p.setAuthUserId(102);
        p.setUsername("owner");
        p.setEmail("owner@mail.com");
        p.setName("Owner");
        p = repository.save(p);

        TokenValidationResult tokenResult = new TokenValidationResult();
        tokenResult.setValid(true);
        tokenResult.setUserId(999);
        tokenResult.setUsername("other-user");
        tokenResult.setRole("USER");

        when(soapAuthClient.validateToken(anyString())).thenReturn(tokenResult);

        String updateBody = """
                { \"name\": \"Hacker\" }
                """;

        mockMvc.perform(put("/users/" + p.getId())
                .header("Authorization", "Bearer sample.jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
