package com.salon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.dto.request.SalonOwnerManagementEditRequest;
import com.salon.dto.response.SalonOwnerManagementResponse;
import com.salon.exception.GlobalExceptionHandler;
import com.salon.exception.SalonOwnerNotFoundException;
import com.salon.service.SalonOwnerManagementService;
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

@WebMvcTest(SalonOwnerManagementController.class)
@Import(GlobalExceptionHandler.class)
class SalonOwnerManagementControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean SalonOwnerManagementService service;

    // ── 200 success ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void editSalonOwner_validRequest_returns200() throws Exception {
        SalonOwnerManagementEditRequest req =
                new SalonOwnerManagementEditRequest("New Name", "New Salon", "8888888888");

        SalonOwnerManagementResponse resp = SalonOwnerManagementResponse.builder()
                .id(1L).ownerName("New Name").salonName("New Salon")
                .phone("8888888888").email("owner@salon.com").city("Hyderabad")
                .build();

        when(service.updateSalonOwner(eq(1L), any())).thenReturn(resp);

        mockMvc.perform(patch("/api/v1/admin/salon-owners/1/edit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerName").value("New Name"))
                .andExpect(jsonPath("$.salonName").value("New Salon"))
                .andExpect(jsonPath("$.phone").value("8888888888"))
                .andExpect(jsonPath("$.email").value("owner@salon.com"));
    }

    // ── 400 blank ownerName ───────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void editSalonOwner_blankOwnerName_returns400() throws Exception {
        SalonOwnerManagementEditRequest req =
                new SalonOwnerManagementEditRequest("", "Salon", "9000000001");

        mockMvc.perform(patch("/api/v1/admin/salon-owners/1/edit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Please provide a valid ownerName"));
    }

    // ── 400 invalid phone ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void editSalonOwner_invalidPhone_returns400() throws Exception {
        SalonOwnerManagementEditRequest req =
                new SalonOwnerManagementEditRequest("Name", "Salon", "123");

        mockMvc.perform(patch("/api/v1/admin/salon-owners/1/edit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Please provide a valid phone"));
    }

    // ── 404 not found ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void editSalonOwner_notFound_returns404() throws Exception {
        SalonOwnerManagementEditRequest req =
                new SalonOwnerManagementEditRequest("Name", "Salon", "9000000001");

        when(service.updateSalonOwner(eq(99L), any()))
                .thenThrow(new SalonOwnerNotFoundException(99L));

        mockMvc.perform(patch("/api/v1/admin/salon-owners/99/edit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── 403 non-admin ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void editSalonOwner_nonAdmin_returns403() throws Exception {
        SalonOwnerManagementEditRequest req =
                new SalonOwnerManagementEditRequest("Name", "Salon", "9000000001");

        mockMvc.perform(patch("/api/v1/admin/salon-owners/1/edit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
