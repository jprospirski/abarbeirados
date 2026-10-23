package uniamerica.abarbeirados.controller;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

final class ControllerErrorAssertions {

    private ControllerErrorAssertions() {
    }

    static void assertApiError(ResultActions resultado, HttpStatus esperado, String mensagem) throws Exception {
        resultado.andExpect(status().is(esperado.value()))
                .andExpect(jsonPath("$.status").value(esperado.value()))
                .andExpect(jsonPath("$.error").value(esperado.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(mensagem));
    }
}
