package com.salon.controller;

import com.salon.dto.response.ScheduleSlotResponse;
import com.salon.entity.SlotStatus;
import com.salon.entity.SlotType;
import com.salon.service.ScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests using MockMvc.
 * Spring Security is bypassed with @WithMockUser.
 */
@WebMvcTest(ScheduleController.class)
class ScheduleControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  ScheduleService scheduleService;

    // Needed by Spring Security auto-config in WebMvcTest
    @MockBean com.salon.security.JwtUtil jwtUtil;
    @MockBean com.salon.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    private ScheduleSlotResponse sampleSlot(String status) {
        return ScheduleSlotResponse.builder()
                .id(1L)
                .date(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .slotType(SlotType.WORKING.name())
                .slotStatus(status)
                .booked(false)
                .past(false)
                .build();
    }

    @Test
    @DisplayName("GET /today → 200 with schedule list")
    @WithMockUser(roles = "CUSTOMER")
    void getTodaySchedule_returns200() throws Exception {
        when(scheduleService.getTodaySchedule(anyLong()))
                .thenReturn(List.of(sampleSlot("AVAILABLE")));

        mockMvc.perform(get("/api/v1/professionals/1/schedule/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].past").value(false));
    }

    @Test
    @DisplayName("GET /{date}/slots → 200 with slot list")
    @WithMockUser(roles = "CUSTOMER")
    void getSlotsByDate_returns200() throws Exception {
        when(scheduleService.getSlotsByDate(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(
                        sampleSlot("AVAILABLE"),
                        sampleSlot("UNAVAILABLE")
                ));

        mockMvc.perform(get("/api/v1/professionals/1/schedule/2026-05-10/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slotStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$[1].slotStatus").value("UNAVAILABLE"));
    }

    @Test
    @DisplayName("GET /today → 200 with empty list when no slots")
    @WithMockUser(roles = "PROFESSIONAL")
    void getTodaySchedule_emptyList_returns200() throws Exception {
        when(scheduleService.getTodaySchedule(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/professionals/99/schedule/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
