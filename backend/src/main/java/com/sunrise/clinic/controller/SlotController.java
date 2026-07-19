package com.sunrise.clinic.controller;

import com.sunrise.clinic.domain.DentistSession;
import com.sunrise.clinic.dto.SessionRequest;
import com.sunrise.clinic.dto.SlotResponse;
import com.sunrise.clinic.mapper.ClinicMapper;
import com.sunrise.clinic.security.ClinicPrincipal;
import com.sunrise.clinic.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Availability: receptionists publish sessions; anyone signed in browses open slots. */
@RestController
@RequestMapping("/api")
public class SlotController {

    private final SlotService slotService;
    private final ClinicMapper mapper;

    public SlotController(SlotService slotService, ClinicMapper mapper) {
        this.slotService = slotService;
        this.mapper = mapper;
    }

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
    public ResponseEntity<Map<String, Object>> publish(@Valid @RequestBody SessionRequest req,
                                                        @AuthenticationPrincipal ClinicPrincipal user) {
        DentistSession session = slotService.publishSession(
                req.dentistId(), req.date(), req.startTime(), req.endTime(),
                req.slotMinutesOrDefault(), user != null ? user.uid() : "system");
        List<SlotResponse> slots = slotService.openSlots(req.dentistId(), req.date())
                .stream().map(mapper::toSlotResponse).toList();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("sessionId", session.getId(), "slots", slots));
    }

    @GetMapping("/availability")
    public List<SlotResponse> availability(@RequestParam String dentistId,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return slotService.openSlots(dentistId, date).stream().map(mapper::toSlotResponse).toList();
    }

    @GetMapping("/availability/week")
    public List<SlotResponse> week(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return slotService.openSlotsBetween(from, to).stream().map(mapper::toSlotResponse).toList();
    }
}
