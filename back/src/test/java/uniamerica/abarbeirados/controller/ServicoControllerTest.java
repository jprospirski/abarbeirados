package uniamerica.abarbeirados.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uniamerica.abarbeirados.dto.servico.ServicoRequest;
import uniamerica.abarbeirados.dto.servico.ServicoResponse;
import uniamerica.abarbeirados.exception.NegocioException;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.service.ServicoService;

import java.math.BigDecimal;
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

@WebMvcTest(ServicoController.class)
class ServicoControllerTest {

    private static final String ROTA = "/api/servicos";
    private static final String CORPO_VALIDO = """
            {"nome": "Corte", "valor": 45.00, "duracaoMinutos": 30, "ativo": true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicoService servicoService;

    private ServicoRequest request;
    private ServicoResponse response;

    @BeforeEach
    void preparar() {
        BigDecimal valor = new BigDecimal("45.00");
        request = new ServicoRequest("Corte", valor, 30, true);
        response = new ServicoResponse(3L, "Corte", valor, 30, true);
    }

    @Test
    void criar_quandoPrecoEDuracaoValidos_retornaServicoCriado() throws Exception {
        // Arrange
        when(servicoService.criar(request)).thenReturn(response);

        // Act
        var resultado = mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO));

        // Assert
        resultado.andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Corte"))
                .andExpect(jsonPath("$.valor").value(45.00));
        verify(servicoService).criar(request);
    }

    @Test
    void listar_quandoFiltrosEnviados_retornaServicosAtivos() throws Exception {
        // Arrange
        when(servicoService.listar("Corte", true)).thenReturn(List.of(response));

        // Act
        var resultado = mockMvc.perform(get(ROTA).param("nome", "Corte").param("apenasAtivos", "true"));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].ativo").value(true));
        verify(servicoService).listar("Corte", true);
    }

    @Test
    void buscarPorId_quandoServicoCadastrado_retornaDetalhes() throws Exception {
        // Arrange
        when(servicoService.buscarPorId(3L)).thenReturn(response);

        // Act
        var resultado = mockMvc.perform(get(ROTA + "/3"));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.duracaoMinutos").value(30));
        verify(servicoService).buscarPorId(3L);
    }

    @Test
    void atualizar_quandoDadosDoServicoValidos_retornaAlteracoes() throws Exception {
        // Arrange
        when(servicoService.atualizar(3L, request)).thenReturn(response);

        // Act
        var resultado = mockMvc.perform(put(ROTA + "/3")
                .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Corte"))
                .andExpect(jsonPath("$.duracaoMinutos").value(30));
        verify(servicoService).atualizar(3L, request);
    }

    @Test
    void excluir_quandoServicoEncontrado_retorna204() throws Exception {
        // Arrange
        Long id = 3L;

        // Act
        var resultado = mockMvc.perform(delete(ROTA + "/{id}", id));

        // Assert
        resultado.andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(servicoService).excluir(id);
    }

    @Test
    void buscarPorId_quandoServicoNaoEncontrado_retornaMensagem404() throws Exception {
        // Arrange
        String mensagem = "Serviço não encontrado com id 99";
        when(servicoService.buscarPorId(99L)).thenThrow(new ResourceNotFoundException(mensagem));

        // Act
        var resultado = mockMvc.perform(get(ROTA + "/99"));

        // Assert
        assertApiError(resultado, HttpStatus.NOT_FOUND, mensagem);
        verify(servicoService).buscarPorId(99L);
    }

    @Test
    void criar_quandoPrecoNulo_retornaErroDoCampoValor() throws Exception {
        // Arrange
        String corpo = """
                {"nome": "Corte", "valor": null, "duracaoMinutos": 30, "ativo": true}
                """;

        // Act
        var resultado = mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(corpo));

        // Assert
        assertApiError(resultado, HttpStatus.BAD_REQUEST, "Erro de validação");
        resultado.andExpect(jsonPath("$.fields.valor").value("O preço é obrigatório"));
        verify(servicoService, never()).criar(any(ServicoRequest.class));
    }

    @Test
    void criar_quandoNegocioExceptionOcorre_retornaMensagem400() throws Exception {
        // Arrange
        String mensagem = "Regra de negócio violada";
        when(servicoService.criar(request)).thenThrow(new NegocioException(mensagem));

        // Act
        var resultado = mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO));

        // Assert
        assertApiError(resultado, HttpStatus.BAD_REQUEST, mensagem);
        verify(servicoService).criar(request);
    }
}
