package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.models.Task;
import absent_minded.absent_minded.services.TaskService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetTasksByProject_success() throws Exception {
        String header = "Bearer demo-token";
        String projectId = "proj1";
        Task t1 = new Task(); t1.setId("1"); t1.setProject(projectId);
        Task t2 = new Task(); t2.setId("2"); t2.setProject(projectId);
        when(taskService.getTasksByProject(eq(header), eq(projectId)))
                .thenReturn(Arrays.asList(t1, t2));

        mockMvc.perform(get("/api/tasks/project/{projectId}", projectId)
                        .header("Authorization", header))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("1"));
    }

    @Test
    public void testGetAllTasks_success() throws Exception {
        String header = "Bearer demo-token";
        Task t = new Task(); t.setId("x");
        when(taskService.getAllTasks(eq(header)))
                .thenReturn(Collections.singletonList(t));

        mockMvc.perform(get("/api/tasks").header("Authorization", header))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("x"));
    }

    @Test
    public void testCreateTasks_success() throws Exception {
        String header = "Bearer demo-token";
        Task t = new Task(); t.setId("a");
        List<Task> input = Collections.singletonList(t);
        when(taskService.createTasks(eq(header), anyList()))
                .thenReturn(input);

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", header)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("a"));
    }

    @Test
    public void testCreateTasks_unauthorized() throws Exception {
        String header = "Bearer bad-token";
        when(taskService.createTasks(eq(header), anyList()))
                .thenThrow(new ResponseStatusException(UNAUTHORIZED, "NO access"));

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", header)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("NO access"));
    }

    @Test
    public void testUpdateTasks_success() throws Exception {
        String header = "Bearer demo-token";
        Task t = new Task(); t.setId("u1");
        List<Task> list = Collections.singletonList(t);
        when(taskService.updateTasks(eq(header), anyList()))
                .thenReturn(list);

        mockMvc.perform(put("/api/tasks")
                        .header("Authorization", header)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(list)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("u1"));
    }

    @Test
    public void testDeleteTasks_success() throws Exception {
        String header = "Bearer demo-token";
        List<String> ids = Arrays.asList("1", "2");
        doNothing().when(taskService).deleteTasks(eq(header), eq(ids));

        mockMvc.perform(delete("/api/tasks")
                        .header("Authorization", header)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isNoContent());
    }

@Test
public void testDeleteTasks_unauthorized() throws Exception {
    String header = "Bearer bad-token";
    doThrow(new ResponseStatusException(UNAUTHORIZED, "NO access"))
            .when(taskService).deleteTasks(eq(header), anyList());

    mockMvc.perform(delete("/api/tasks")
                    .header("Authorization", header)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[\"x\"]"))
            .andExpect(status().isUnauthorized())
            .andExpect(status().reason("NO access"));
}
}