// UserControllerTest.java
package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.models.User;
import absent_minded.absent_minded.models.UserEvent;
import absent_minded.absent_minded.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateUser_success() throws Exception {
        String header = "Bearer demo-token";
        User input = new User();
        input.setPlan("pro");

        User created = new User();
        created.setId("u123");
        created.setPlan("pro");
        created.setTokenUsed(0);
        created.setEvents(Collections.emptyList());
        created.setCreated(LocalDateTime.of(2025,7,5,20,30,0));

        when(userService.createUser(eq(header)))
                .thenReturn(created);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", header)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("u123"))
                .andExpect(jsonPath("$.plan").value("pro"))
                .andExpect(jsonPath("$.tokenUsed").value(0))
                .andExpect(jsonPath("$.created").value("2025-07-05T20:30:00"));
    }

    @Test
    public void testCreateUser_conflict() throws Exception {
        String header = "Bearer demo-token";
        User input = new User();
        input.setPlan("free");

        when(userService.createUser(eq(header)))
                .thenThrow(new ResponseStatusException(CONFLICT, "User already exists"));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", header)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isConflict())
                .andExpect(status().reason("User already exists"));
    }

    @Test
    public void testGetUserById_success() throws Exception {
        String header = "Bearer demo-token";
        User user = new User();
        user.setId("demo@gmail.com");
        user.setPlan("free");
        user.setTokenUsed(5);
        user.setEvents(Collections.emptyList());
        user.setCreated(LocalDateTime.of(2025,7,5,21,0,0));

        when(userService.getUserById(eq(header)))
                .thenReturn(user);

        mockMvc.perform(get("/api/users").header("Authorization", header))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("demo@gmail.com"))
                .andExpect(jsonPath("$.plan").value("free"))
                .andExpect(jsonPath("$.tokenUsed").value(5));
    }

    @Test
    public void testGetUserById_notFound() throws Exception {
        String header = "Bearer demo-token";

        when(userService.getUserById(eq(header)))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "User not found"));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", header))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("User not found"));
    }

    @Test
    public void testGetUserById_unauthorized() throws Exception {
        String header = "Bearer invalid-token";

        when(userService.getUserById(eq(header)))
                .thenThrow(new ResponseStatusException(UNAUTHORIZED, "Missing or invalid Authorization header"));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", header))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Missing or invalid Authorization header"));
    }

    @Test
    public void testUpdateUserPlan_upgradeOk() throws Exception {
        String header = "Bearer demo-token";
        Map<String,String> body = Collections.singletonMap("plan","pro");

        User updated = new User();
        updated.setId("u123");
        updated.setPlan("pro");
        updated.setTokenUsed(0);
        UserEvent upgradeEvent = new UserEvent("Upgrade to pro plan");
        updated.setEvents(List.of(upgradeEvent));

        when(userService.updateUserPlan(eq(header), anyMap())).thenReturn(updated);

        mockMvc.perform(put("/api/users/plan")
                        .header("Authorization", header)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("pro"))
                .andExpect(jsonPath("$.events[0].event").value("Upgrade to pro plan"));
    }

    @Test
    public void testUpdateUserPlan_noChangeOk() throws Exception {
        String header = "Bearer demo-token";
        Map<String,String> body = Collections.singletonMap("plan","free");

        User unchanged = new User();
        unchanged.setId("u123");
        unchanged.setPlan("free");
        unchanged.setTokenUsed(0);
        unchanged.setEvents(Collections.emptyList());

        when(userService.updateUserPlan(eq(header), anyMap())).thenReturn(unchanged);

        mockMvc.perform(put("/api/users/plan")
                        .header("Authorization", header)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("free"))
                .andExpect(jsonPath("$.events").isEmpty());
    }

    @Test
    public void testUpdateUserPlan_notFound() throws Exception {
        String header = "Bearer demo-token";
        Map<String,String> body = Collections.singletonMap("plan","pro");

        when(userService.updateUserPlan(eq(header), anyMap()))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "User not found"));

        mockMvc.perform(put("/api/users/plan")
                        .header("Authorization", header)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("User not found"));
    }

    @Test
    public void testUpdateUserPlan_unauthorized() throws Exception {
        String header = "Bearer invalid-token";
        Map<String,String> body = Collections.singletonMap("plan","pro");

        when(userService.updateUserPlan(eq(header), anyMap()))
                .thenThrow(new ResponseStatusException(UNAUTHORIZED, "Missing or invalid Authorization header"));

        mockMvc.perform(put("/api/users/plan")
                        .header("Authorization", header)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Missing or invalid Authorization header"));
    }

    @Test
    public void testDeleteUser_success() throws Exception {
        String header = "Bearer demo-token";
        doNothing().when(userService).deleteUser(eq(header));

        mockMvc.perform(delete("/api/users")
                        .header("Authorization", header))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteUser_notFound() throws Exception {
        String header = "Bearer demo-token";
        doThrow(new ResponseStatusException(NOT_FOUND, "User not found"))
                .when(userService).deleteUser(eq(header));

        mockMvc.perform(delete("/api/users")
                        .header("Authorization", header))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("User not found"));
    }

    @Test
    public void testDeleteUser_unauthorized() throws Exception {
        String header = "Bearer invalid-token";
        doThrow(new ResponseStatusException(UNAUTHORIZED, "Missing or invalid Authorization header"))
                .when(userService).deleteUser(eq(header));

        mockMvc.perform(delete("/api/users")
                        .header("Authorization", header))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Missing or invalid Authorization header"));
    }
}