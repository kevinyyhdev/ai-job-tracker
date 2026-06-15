package com.kevin.jobtracker.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @RestController
    static class ProbeController {
        record ProbeRequest(@NotBlank String name) {}

        @GetMapping("/probe/not-found")
        void notFound() { throw new ResourceNotFoundException("Resource not found"); }

        @GetMapping("/probe/duplicate")
        void duplicate() { throw new DuplicateResourceException("Already exists"); }

        @GetMapping("/probe/business-rule")
        void businessRule() { throw new BusinessRuleException("Rule violated"); }

        @PostMapping("/probe/validation")
        void validation(@Valid @RequestBody ProbeRequest req) {}

        @GetMapping("/probe/unknown")
        void unknown() { throw new RuntimeException("unexpected"); }
    }

    @Test
    void notFoundReturns404() throws Exception {
        mockMvc.perform(get("/probe/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void duplicateReturns409() throws Exception {
        mockMvc.perform(get("/probe/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void businessRuleReturns422() throws Exception {
        mockMvc.perform(get("/probe/business-rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void validationErrorReturns422WithFieldDetails() throws Exception {
        mockMvc.perform(post("/probe/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.name").exists());
    }

    @Test
    void unknownExceptionReturns500WithSafeMessage() throws Exception {
        mockMvc.perform(get("/probe/unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("An unexpected error occurred"));
    }
}
