package uniamerica.abarbeirados.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uniamerica.abarbeirados.dto.cliente.ClienteRequest;
import uniamerica.abarbeirados.dto.cliente.ClienteResponse;
import uniamerica.abarbeirados.exception.NegocioException;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.service.ClienteService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uniamerica.abarbeirados.controller.ControllerErrorAssertions.assertApiError;

@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    private static final String ROTA = "/api/clientes";
    private static final String CORPO_VALIDO = """
            {"nome": "Ana", "email": "ana@example.com", "telefone": "45999999999"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    private ClienteRequest request;
    private ClienteResponse response;

    @BeforeEach
    void preparar() {
        request = new ClienteRequest("Ana", "ana@example.com", "45999999999");
        response = new ClienteResponse(1L, "Ana", "ana@example.com", "45999999999",
                LocalDateTime.of(2026, 10, 22, 12, 0));
    }

    @Test
    void criar_quandoClienteValido_retornaDadosDoCadastro() throws Exception {
        // Arrange
        when(clienteService.criar(request)).thenReturn(response);

        // Act
        var resultado = mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO));

        // Assert
        resultado.andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@example.com"));
        verify(clienteService).criar(request);
    }

    @Test
    void listar_quandoNomeInformado_retornaClientesFiltrados() throws Exception {
        // Arrange
        when(clienteService.listar("Ana")).thenReturn(List.of(response));

        // Act
        var resultado = mockMvc.perform(get(ROTA).param("nome", "Ana"));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].telefone").value("45999999999"));
        verify(clienteService).listar("Ana");
    }

    @Test
    void buscarPorId_quandoClienteExiste_retornaCadastroEncontrado() throws Exception {
        // Arrange
        when(clienteService.buscarPorId(1L)).thenReturn(response);

        // Act
        var resultado = mockMvc.perform(get(ROTA + "/1"));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dataCadastro").value("2026-10-22T12:00:00"));
        verify(clienteService).buscarPorId(1L);
    }

    @Test
    void atualizar_quandoClienteValido_retornaContatoAtualizado() throws Exception {
        // Arrange
        when(clienteService.atualizar(1L, request)).thenReturn(response);

        // Act
        var resultado = mockMvc.perform(put(ROTA + "/1")
                .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.telefone").value("45999999999"));
        verify(clienteService).atualizar(1L, request);
    }

    @Test
    void excluir_quandoClienteExiste_retornaRespostaVazia() throws Exception {
        // Arrange
        Long id = 1L;

        // Act
        var resultado = mockMvc.perform(delete(ROTA + "/{id}", id));

        // Assert
        resultado.andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(clienteService).excluir(id);
    }

    @Test
    void buscarPorId_quandoClienteNaoExiste_retornaApiErrorComMensagem() throws Exception {
        // Arrange
        String mensagem = "Cliente não encontrado com id 99";
        when(clienteService.buscarPorId(99L)).thenThrow(new ResourceNotFoundException(mensagem));

        // Act
        var resultado = mockMvc.perform(get(ROTA + "/99"));

        // Assert
        assertApiError(resultado, HttpStatus.NOT_FOUND, mensagem);
        verify(clienteService).buscarPorId(99L);
    }

    @Test
    void criar_quandoTelefoneVazio_retornaErroDeValidacao() throws Exception {
        // Arrange
        String corpo = """
                {"nome": "Ana", "email": "ana@example.com", "telefone": ""}
                """;

        // Act
        var resultado = mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(corpo));

        // Assert
        assertApiError(resultado, HttpStatus.BAD_REQUEST, "Erro de validação");
        resultado.andExpect(jsonPath("$.fields.telefone").value("O telefone é obrigatório"));
        verify(clienteService, never()).criar(any(ClienteRequest.class));
    }

    @Test
    void criar_quandoServiceRejeitaRegraDeNegocio_retornaApiError() throws Exception {
        // Arrange
        String mensagem = "Regra de negócio violada";
        when(clienteService.criar(request)).thenThrow(new NegocioException(mensagem));

        // Act
        var resultado = mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO));

        // Assert
        assertApiError(resultado, HttpStatus.BAD_REQUEST, mensagem);
        verify(clienteService).criar(request);
    }
}
