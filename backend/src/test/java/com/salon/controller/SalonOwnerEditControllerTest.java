package com.salon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.dto.request.SalonOwnerEditRequest;
import com.salon.dto.response.SalonOwnerEditResponse;
import com.salon.exception.GlobalExceptionHandler;
import com.salon.exception.ResourceNotFoundException;
import com.salon.service.SalonOwnerEditService;
import com.salon.service.SuspensionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for UserManagementController — only the web layer is loaded.
 * Spring Security is active; we use @WithMockUser to simulate an ADMIN.
 */
@WebMvcTest(UserManagementController.class)
@Import(GlobalExceptionHandler.class)
class SalonOwnerEditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SalonOwnerEditService salonOwnerEditService;

    @MockBean
    private SuspensionService suspensionService;

    // ── 200 success ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void editSalonOwner_validRequest_returns200() throws Exception {
        SalonOwnerEditRequest request = new SalonOwnerEditRequest("New Name", "8888888888", "Old Salon");

        SalonOwnerEditResponse response = SalonOwnerEditResponse.builder()
                .id(1L)
                .name("New Name")
                .phone("8888888888")
                .email("owner@salon.com")
                .city("Hyderabad")
                .role("SALON_OWNER")
                .additionalInfo("Old Salon")
                .build();

        when(salonOwnerEditService.updateSalonOwner(eq(1L), any(SalonOwnerEditRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/users/1/edit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.phone").value("8888888888"))
                .andExpect(jsonPath("$.email").value("owner@salon.com"))
                .andExpect(jsonPath("$.role").value("SALON_OWNER"));
    }

    // ── 400 validation error ──────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void editSalonOwner_blankName_returns400() throws Exception {
        SalonOwnerEditRequest request = new SalonOwnerEditRequest("", "8888888888", "Old Salon");

        mockMvc.perform(patch("/api/v1/admin/users/1/edit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Please provide a valid name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editSalonOwner_invalidPhone_returns400() throws Exception {
        SalonOwnerEditRequest request = new SalonOwnerEditRequest("Valid Name", "123", "Old Salon");

        mockMvc.perform(patch("/api/v1/admin/users/1/edit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Please provide a valid phone"));
    }

    // ── 404 not found ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void editSalonOwner_notFound_returns404() throws Exception {
        SalonOwnerEditRequest request = new SalonOwnerEditRequest("Name", "9000000001", "Old Salon");

        when(salonOwnerEditService.updateSalonOwner(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Salon Owner not found with id: 99"));

        mockMvc.perform(patch("/api/v1/admin/users/99/edit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── 403 non-admin ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void editSalonOwner_nonAdmin_returns403() throws Exception {
        SalonOwnerEditRequest request = new SalonOwnerEditRequest("Name", "9000000001", "Old Salon");

        mockMvc.perform(patch("/api/v1/admin/users/1/edit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
