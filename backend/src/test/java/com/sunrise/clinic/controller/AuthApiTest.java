package com.sunrise.clinic.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TC-CLI-SEC-05..07 — lockout via the API + admin-only unlock. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired MockMvc mvc;

    private static final String BODY = "{\"email\":\"lockme@x.lk\"}";

    @Test
    void lockoutAfterFiveFailuresAndAdminUnlock() throws Exception {
        // 5 failed logins → account locked
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/failed-login")
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/api/auth/lock-status").param("email", "lockme@x.lk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(true));

        // A receptionist cannot unlock → 403
        mvc.perform(post("/api/auth/unlock")
                        .header("X-User-Uid", "rec").header("X-User-Role", "RECEPTIONIST")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());

        // An admin unlocks → account is open again
        mvc.perform(post("/api/auth/unlock")
                        .header("X-User-Uid", "admin").header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false));
    }
}
