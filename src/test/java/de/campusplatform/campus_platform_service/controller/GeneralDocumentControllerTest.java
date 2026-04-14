package de.campusplatform.campus_platform_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.campusplatform.campus_platform_service.dto.UploadGeneralDocumentRequest;
import de.campusplatform.campus_platform_service.model.GeneralDocument;
import de.campusplatform.campus_platform_service.repository.GeneralDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GeneralDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GeneralDocumentRepository generalDocumentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        generalDocumentRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void testUploadAndDownload() throws Exception {
        String content = "Hello World";
        String base64Content = Base64.getEncoder().encodeToString(content.getBytes());
        UploadGeneralDocumentRequest request = new UploadGeneralDocumentRequest(
                "Test Doc",
                "test.txt",
                "text/plain",
                base64Content,
                (long) content.length()
        );

        // 1. Upload
        mockMvc.perform(post("/api/admin/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Test Doc"))
                .andExpect(jsonPath("$.id").exists());

        Long id = generalDocumentRepository.findAll().get(0).getId();

        // 2. List
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("Test Doc"));

        // 3. Download
        mockMvc.perform(get("/api/documents/" + id + "/download"))
                .andExpect(status().isOk())
                .andExpect(content().string(content))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""));

        // 4. Delete
        mockMvc.perform(delete("/api/admin/documents/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
