package com.sunrise.clinic.controller;

import com.sunrise.clinic.dto.BillResponse;
import com.sunrise.clinic.exception.ResourceNotFoundException;
import com.sunrise.clinic.mapper.ClinicMapper;
import com.sunrise.clinic.repository.BillRepository;
import com.sunrise.clinic.security.ClinicPrincipal;
import com.sunrise.clinic.service.BillingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Bill generation (Strategy + revenue split) and retrieval. */
@RestController
@RequestMapping("/api/appointments/{appointmentNo}/bill")
public class BillController {

    private final BillingService billingService;
    private final BillRepository bills;
    private final ClinicMapper mapper;

    public BillController(BillingService billingService, BillRepository bills, ClinicMapper mapper) {
        this.billingService = billingService;
        this.bills = bills;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
    public ResponseEntity<BillResponse> generate(@PathVariable String appointmentNo,
                                                 @AuthenticationPrincipal ClinicPrincipal user) {
        var bill = billingService.generateBill(appointmentNo, user != null ? user.uid() : "system");
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toBillResponse(bill));
    }

    @GetMapping
    public BillResponse get(@PathVariable String appointmentNo) {
        return bills.findByAppointmentNo(appointmentNo).map(mapper::toBillResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No bill for appointment " + appointmentNo));
    }
}
