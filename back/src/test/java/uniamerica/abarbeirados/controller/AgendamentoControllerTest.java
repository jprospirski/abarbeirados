package uniamerica.abarbeirados.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uniamerica.abarbeirados.dto.agendamento.AgendaDoDiaResponse;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoRequest;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoResponse;
import uniamerica.abarbeirados.dto.agendamento.AtualizarStatusRequest;
import uniamerica.abarbeirados.exception.NegocioException;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.model.StatusAgendamento;
import uniamerica.abarbeirados.service.AgendamentoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uniamerica.abarbeirados.controller.ControllerErrorAssertions.assertApiError;

@WebMvcTest(AgendamentoController.class)
class AgendamentoControllerTest {

    private static final String ROTA = "/api/agendamentos";
    private static final LocalDateTime DATA_HORA = LocalDateTime.of(2099, 10, 23, 10, 30);
    private static final String CORPO_VALIDO = """
            {
              "clienteId": 1,
              "barbeiroId": 2,
              "servicoId": 3,
              "dataHora": "2099-10-23T10:30:00",
              "observacoes": "Corte curto"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgendamentoService agendamentoService;

    private AgendamentoRequest request;
    private AgendamentoResponse response;

    @BeforeEach
    void preparar() {
        request = new AgendamentoRequest(1L, 2L, 3L, DATA_HORA, "Corte curto");
        response = resposta(StatusAgendamento.AGENDADO);
    }

    @Test
    void criar_quandoDadosValidos_retornaAgendamentoCriado() throws Exception {
        // Arrange
        when(agendamentoService.criar(request)).thenReturn(response);

        // Act
        var resultado = mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO));

        // Assert
        resultado.andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteNome").value("Ana"))
                .andExpect(jsonPath("$.status").value("AGENDADO"));
        verify(agendamentoService).criar(request);
    }

    @Test
    void listar_quandoFiltrosInformados_retornaAgendamentosFiltrados() throws Exception {
        // Arrange
        LocalDate dia = DATA_HORA.toLocalDate();
        when(agendamentoService.listar("Ana", dia)).thenReturn(List.of(response));

        // Act
        var resultado = mockMvc.perform(get(ROTA).param("busca", "Ana").param("data", dia.toString()));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4))
                .andExpect(jsonPath("$.length()").value(1));
        verify(agendamentoService).listar("Ana", dia);
    }

    @Test
    void listar_quandoFiltrosAusentes_retornaListaVazia() throws Exception {
        // Arrange
        when(agendamentoService.listar(null, null)).thenReturn(List.of());

        // Act
        var resultado = mockMvc.perform(get(ROTA));

        // Assert
        resultado.andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(agendamentoService).listar(null, null);
    }

    @Test
    void agenda_quandoHaAgendamentos_retornaDiasAgrupados() throws Exception {
        // Arrange
        var agenda = new AgendaDoDiaResponse(DATA_HORA.toLocalDate(), List.of(response));
        when(agendamentoService.agendaAgrupadaPorDia()).thenReturn(List.of(agenda));

        // Act
        var resultado = mockMvc.perform(get(ROTA + "/agenda"));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dia").value("2099-10-23"))
                .andExpect(jsonPath("$[0].agendamentos[0].id").value(4));
        verify(agendamentoService).agendaAgrupadaPorDia();
    }

    @Test
    void buscarPorId_quandoAgendamentoExiste_retornaAgendamento() throws Exception {
        // Arrange
        when(agendamentoService.buscarPorId(4L)).thenReturn(response);

        // Act
        var resultado = mockMvc.perform(get(ROTA + "/4"));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.servicoNome").value("Corte"));
        verify(agendamentoService).buscarPorId(4L);
    }

    @Test
    void atualizar_quandoDadosValidos_retornaAgendamentoAtualizado() throws Exception {
        // Arrange
        when(agendamentoService.atualizar(4L, request)).thenReturn(response);

        // Act
        var resultado = mockMvc.perform(put(ROTA + "/4")
                .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.observacoes").value("Corte curto"));
        verify(agendamentoService).atualizar(4L, request);
    }

    @Test
    void atualizarStatus_quandoStatusValido_retornaNovoStatus() throws Exception {
        // Arrange
        var novoStatus = new AtualizarStatusRequest(StatusAgendamento.CONFIRMADO);
        when(agendamentoService.atualizarStatus(4L, novoStatus)).thenReturn(resposta(StatusAgendamento.CONFIRMADO));
        String corpo = """
                {"status": "CONFIRMADO"}
                """;

        // Act
        var resultado = mockMvc.perform(patch(ROTA + "/4/status").contentType(MediaType.APPLICATION_JSON).content(corpo));

        // Assert
        resultado.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMADO"));
        verify(agendamentoService).atualizarStatus(4L, novoStatus);
    }

    @Test
    void excluir_quandoAgendamentoExiste_retornaSemConteudo() throws Exception {
        // Arrange
        Long id = 4L;

        // Act
        var resultado = mockMvc.perform(delete(ROTA + "/{id}", id));

        // Assert
        resultado.andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(agendamentoService).excluir(id);
    }

    @Test
    void buscarPorId_quandoAgendamentoNaoExiste_retornaApiError404() throws Exception {
        // Arrange
        String mensagem = "Agendamento não encontrado com id 99";
        when(agendamentoService.buscarPorId(99L)).thenThrow(new ResourceNotFoundException(mensagem));

        // Act
        var resultado = mockMvc.perform(get(ROTA + "/99"));

        // Assert
        assertApiError(resultado, HttpStatus.NOT_FOUND, mensagem);
        verify(agendamentoService).buscarPorId(99L);
    }

    @Test
    void criar_quandoClienteNulo_retornaApiErrorDeValidacao() throws Exception {
        // Arrange
        String corpo = """
                {"clienteId": null, "barbeiroId": 2, "servicoId": 3, "dataHora": "2099-10-23T10:30:00"}
                """;

        // Act
        var resultado = mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(corpo));

        // Assert
        assertApiError(resultado, HttpStatus.BAD_REQUEST, "Erro de validação");
        resultado.andExpect(jsonPath("$.fields.clienteId").value("O cliente é obrigatório"));
        verify(agendamentoService, never()).criar(any(AgendamentoRequest.class));
    }

    @Test
    void atualizarStatus_quandoStatusNulo_retornaCampoObrigatorio() throws Exception {
        // Arrange
        String corpo = """
                {"status": null}
                """;

        // Act
        var resultado = mockMvc.perform(patch(ROTA + "/4/status").contentType(MediaType.APPLICATION_JSON).content(corpo));

        // Assert
        assertApiError(resultado, HttpStatus.BAD_REQUEST, "Erro de validação");
        resultado.andExpect(jsonPath("$.fields.status").value("O status é obrigatório"));
        verify(agendamentoService, never()).atualizarStatus(any(Long.class), any(AtualizarStatusRequest.class));
    }

    @Test
    void criar_quandoRegraDeNegocioViolada_retornaApiError400() throws Exception {
        // Arrange
        String mensagem = "Regra de negócio violada";
        when(agendamentoService.criar(request)).thenThrow(new NegocioException(mensagem));

        // Act
        var resultado = mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO));

        // Assert
        assertApiError(resultado, HttpStatus.BAD_REQUEST, mensagem);
        verify(agendamentoService).criar(request);
    }

    @Test
    void criar_quandoJsonMalformado_retornaApiError400() throws Exception {
        // Arrange
        String corpo = """
                {"clienteId":
                """;

        // Act
        var resultado = mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(corpo));

        // Assert
        assertApiError(resultado, HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado");
        verify(agendamentoService, never()).criar(any(AgendamentoRequest.class));
    }

    @Test
    void buscarPorId_quandoIdIncompativel_retornaApiError400() throws Exception {
        // Arrange
        String id = "invalido";

        // Act
        var resultado = mockMvc.perform(get(ROTA + "/{id}", id));

        // Assert
        assertApiError(resultado, HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro 'id': invalido");
        verify(agendamentoService, never()).buscarPorId(any(Long.class));
    }

    @Test
    void criar_quandoIntegridadeViolada_retornaApiError409() throws Exception {
        // Arrange
        when(agendamentoService.criar(request)).thenThrow(new DataIntegrityViolationException("Registro vinculado"));

        // Act
        var resultado = mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO));

        // Assert
        assertApiError(resultado, HttpStatus.CONFLICT,
                "O registro não pode ser alterado ou excluído porque está sendo usado por outro registro");
        verify(agendamentoService).criar(request);
    }

    @Test
    void requisitar_quandoRotaNaoExiste_retornaApiError404() throws Exception {
        // Arrange
        String rota = "/api/rota-inexistente";

        // Act
        var resultado = mockMvc.perform(get(rota));

        // Assert
        assertApiError(resultado, HttpStatus.NOT_FOUND, "Rota não encontrada: api/rota-inexistente");
        verify(agendamentoService, never()).listar(any(), any());
    }

    private AgendamentoResponse resposta(StatusAgendamento status) {
        return new AgendamentoResponse(4L, 1L, "Ana", "45999999999", 2L, "Carlos", 3L, "Corte",
                new BigDecimal("45.00"), 30, DATA_HORA, status, "Corte curto");
    }
}
