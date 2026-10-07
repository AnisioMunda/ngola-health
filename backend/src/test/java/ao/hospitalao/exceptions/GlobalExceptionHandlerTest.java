package ao.hospitalao.exceptions;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private static LocalValidatorFactoryBean validatorFactory;
    private MockMvc mockMvc;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.afterPropertiesSet();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validatorFactory)
            .build();
    }

    @Test
    void returnsProblemDetailForInvalidInput() throws Exception {
        mockMvc.perform(post("/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.valueOf("application/problem+json")
            ))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Pedido inválido"))
            .andExpect(jsonPath("$.detail").value("Um ou mais campos são inválidos."))
            .andExpect(jsonPath("$.errors.name").value("Valor inválido."));
    }

    @Test
    void returnsProblemDetailForMissingResources() throws Exception {
        mockMvc.perform(get("/missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
            .andExpect(jsonPath("$.detail").value("O recurso solicitado não foi encontrado."));
    }

    @Test
    void returnsProblemDetailForConflicts() throws Exception {
        mockMvc.perform(get("/conflict"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.title").value("Conflito"))
            .andExpect(jsonPath("$.detail").value("Já existe um registo com os dados informados."));
    }

    @Test
    void hidesUnexpectedExceptionDetailsFromProblemDetail() throws Exception {
        mockMvc.perform(get("/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.title").value("Erro interno"))
            .andExpect(jsonPath("$.detail")
                .value("Ocorreu um erro interno. Tente novamente mais tarde."))
            .andExpect(content().string(not(containsString("database credentials"))));
    }

    @RestController
    static class TestController {

        @PostMapping("/validation")
        void validation(@Valid @RequestBody Request request) {
        }

        @GetMapping("/missing")
        void missing() {
            throw new EntityNotFoundException("sensitive internal identifier");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new EmailAlreadyExistsException("sensitive account details");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("database credentials");
        }
    }

    record Request(@NotBlank String name) {
    }
}
