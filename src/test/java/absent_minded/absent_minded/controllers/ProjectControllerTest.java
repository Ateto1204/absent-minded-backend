package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.repositories.ProjectRepository;
import absent_minded.absent_minded.services.AuthService;
import absent_minded.absent_minded.services.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectRepository projectRepository;

    @MockitoBean
    private AuthService auth;
    @MockitoBean
    private ProjectService projectService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllProjects() throws Exception {
        Project p1 = new Project();
        p1.setId("1");
        p1.setName("Project A");
        p1.setOwnerId("user1");

        Project p2 = new Project();
        p2.setId("2");
        p2.setName("Project B");
        p2.setOwnerId("user2");

        Project p3 = new Project();
        p3.setId("3");
        p3.setName("Project C");
        p3.setOwnerId("user1");

        when(auth.emailFromAuthHeader("Bearer test-token")).thenReturn("user1");
        when(projectRepository.findAllByOwnerId("user1")).thenReturn(Arrays.asList(p1, p3));
        when(projectRepository.findAllByParticipantsContains("user1")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/projects").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].ownerId").value("user1"))
                .andExpect(jsonPath("$[1].ownerId").value("user1"));
    }

    @Test
    void testGetProjectById() throws Exception {
        Project project = new Project();
        project.setId("1");
        project.setName("Project A");
        project.setOwnerId("user1");
        project.setParticipants(List.of("user2"));

        when(auth.emailFromAuthHeader("Bearer test-token")).thenReturn("user1");
        when(projectService.getProjectById("Bearer test-token", "1")).thenReturn(project);

        mockMvc.perform(get("/api/projects/1")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Project A"));
    }

    @Test
    public void testCreateProject() throws Exception {
        Project input = new Project();
        input.setName("New Project");
        // ownerId will be set by controller
        Project saved = new Project();
        saved.setId("123");
        saved.setName("New Project");
        saved.setOwnerId("user1");

        when(auth.emailFromAuthHeader("Bearer test-token")).thenReturn("user1");
        when(projectRepository.save(any(Project.class))).thenReturn(saved);

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.name").value("New Project"))
                .andExpect(jsonPath("$.ownerId").value("user1"));
    }

    @Test
    public void testUpdateProject() throws Exception {
        Project existing = new Project();
        existing.setId("123");
        existing.setName("Old Name");
        existing.setOwnerId("user1");

        Project updated = new Project();
        updated.setId("123");
        updated.setName("Updated Name");
        updated.setOwnerId("user1");

        when(auth.emailFromAuthHeader("Bearer test-token")).thenReturn("user1");
        when(projectRepository.findByIdAndOwnerId("123", "user1")).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenReturn(updated);

        mockMvc.perform(put("/api/projects/123")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    public void testDeleteProject() throws Exception {
        when(auth.emailFromAuthHeader("Bearer test-token")).thenReturn("user1");
        when(projectRepository.deleteByIdAndOwnerId("1", "user1")).thenReturn(1);

        mockMvc.perform(delete("/api/projects/1").header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testAddParticipant() throws Exception {
        Project project = new Project();
        project.setId("1");
        project.setName("Project A");
        project.setOwnerId("user1");
        project.setParticipants(new ArrayList<>());

        Project withNew = new Project();
        withNew.setId("1");
        withNew.setName("Project A");
        withNew.setOwnerId("user1");
        withNew.setParticipants(Arrays.asList("user2"));

        when(auth.emailFromAuthHeader("Bearer test-token")).thenReturn("user1");
        when(projectRepository.findById("1")).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(withNew);

        Map<String, String> body = Collections.singletonMap("email", "user2");
        mockMvc.perform(post("/api/projects/1/participants")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants[0]").value("user2"));
    }

    @Test
    public void testRemoveParticipant() throws Exception {
        Project project = new Project();
        project.setId("1");
        project.setName("Project A");
        project.setOwnerId("user1");
        project.setParticipants(new ArrayList<>(Arrays.asList("user2", "user3")));

        Project afterRemoval = new Project();
        afterRemoval.setId("1");
        afterRemoval.setName("Project A");
        afterRemoval.setOwnerId("user1");
        afterRemoval.setParticipants(Collections.singletonList("user3"));

        when(auth.emailFromAuthHeader("Bearer test-token")).thenReturn("user1");
        when(projectRepository.findById("1")).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(afterRemoval);

        Map<String, String> body = Collections.singletonMap("email", "user2");
        mockMvc.perform(delete("/api/projects/1/participants")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.size()").value(1))
                .andExpect(jsonPath("$.participants[0]").value("user3"));
    }
}