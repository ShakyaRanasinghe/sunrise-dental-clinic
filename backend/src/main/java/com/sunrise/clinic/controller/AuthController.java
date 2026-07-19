package com.sunrise.clinic.controller;

import com.sunrise.clinic.security.LoginAttemptService;
import com.sunrise.clinic.security.LoginAttemptService.LockStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Login-attempt tracking for the account-lockout feature. The frontend reports login
 * outcomes here; the backend locks an identity after too many failures and only an admin
 * (or the 24h timeout) can unlock it.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginAttemptService lockService;

    public AuthController(LoginAttemptService lockService) {
        this.lockService = lockService;
    }

    @PostMapping("/failed-login")
    public LockStatus failedLogin(@RequestBody Map<String, String> body) {
        return lockService.recordFailure(body.get("email"));
    }

    @PostMapping("/successful-login")
    public LockStatus successfulLogin(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        lockService.recordSuccess(email);
        return lockService.status(email);
    }

    @GetMapping("/lock-status")
    public LockStatus lockStatus(@RequestParam String email) {
        return lockService.status(email);
    }

    @PostMapping("/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public LockStatus unlock(@RequestBody Map<String, String> body) {
        lockService.reset(body.get("email"));
        return lockService.status(body.get("email"));
    }
}
