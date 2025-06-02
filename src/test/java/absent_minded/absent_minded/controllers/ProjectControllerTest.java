package absent_minded.absent_minded.controllers;

import org.springframework.test.context.ActiveProfiles;
import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.repositories.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectRepository projectRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllProjects() throws Exception {
        Project p1 = new Project();
        p1.setId("1");
        p1.setName("Project A");
        p1.setUserId("user1");

        Project p2 = new Project();
        p2.setId("2");
        p2.setName("Project B");
        p2.setUserId("user2");

        Project p3 = new Project();
        p3.setId("3");
        p3.setName("Project C");
        p3.setUserId("user1");

        when(projectRepository.findAll()).thenReturn(Arrays.asList(p1, p2, p3));

        System.out.println("Returned Projects JSON: " + objectMapper.writeValueAsString(Arrays.asList(p1, p2, p3)));

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(3))
                .andExpect(jsonPath("$[0].userId").value("user1"))
                .andExpect(jsonPath("$[1].userId").value("user2"))
                .andExpect(jsonPath("$[2].userId").value("user1"));
    }

    @Test
    public void testGetProjectById() throws Exception {
        Project p = new Project();
        p.setId("1");
        p.setName("Project A");
        p.setUserId("user1");

        when(projectRepository.findById("1")).thenReturn(Optional.of(p));

        System.out.println("Returned Project JSON: " + objectMapper.writeValueAsString(p));

        mockMvc.perform(get("/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Project A"));
    }

    @Test
    public void testCreateProject() throws Exception {
        Project input = new Project();
        input.setName("New Project");
        input.setUserId("user1");

        Project saved = new Project();
        saved.setId("123");
        saved.setName("New Project");
        saved.setUserId("user1");

        when(projectRepository.save(any(Project.class))).thenReturn(saved);

        System.out.println("Input JSON: " + objectMapper.writeValueAsString(input));
        System.out.println("Expected Saved JSON: " + objectMapper.writeValueAsString(saved));

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.name").value("New Project"));
    }

    @Test
    public void testDeleteProject() throws Exception {
        System.out.println("DELETE /projects/1 called");
        mockMvc.perform(delete("/projects/1"))
                .andExpect(status().isOk());
    }

    @Test
    public void testUpdateProject() throws Exception {
        Project existing = new Project();
        existing.setId("123");
        existing.setName("Old Name");
        existing.setUserId("user1");

        Project updated = new Project();
        updated.setId("123");
        updated.setName("Updated Name");
        updated.setUserId("user1");

        when(projectRepository.findById("123")).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenReturn(updated);

        System.out.println("Update Project - Input: " + objectMapper.writeValueAsString(updated));

        mockMvc.perform(put("/projects/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }
}