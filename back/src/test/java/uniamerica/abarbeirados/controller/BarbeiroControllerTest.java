package uniamerica.abarbeirados.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uniamerica.abarbeirados.dto.barbeiro.BarbeiroRequest;
import uniamerica.abarbeirados.dto.barbeiro.BarbeiroResponse;
import uniamerica.abarbeirados.dto.barbeiro.ServicoResumoResponse;
import uniamerica.abarbeirados.dto.servico.ServicoResponse;
import uniamerica.abarbeirados.exception.NegocioException;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.service.BarbeiroService;

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

@WebMvcTest(BarbeiroController.class)
class BarbeiroControllerTest {

    private static final String ROTA = "/api/barbeiros";
    private static final String CORPO_VALIDO = """
            {"nome": "Carlos", "servicoIds": [3], "ativo": true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BarbeiroService barbeiroService;

    private BarbeiroRequest request;
    private BarbeiroResponse response;

    @BeforeEach
    void preparar() {
        request = new BarbeiroRequest("Carlos", List.of(3L), true);
        response = new BarbeiroResponse(2L, "Carlos", true, List.of(new ServicoResumoResponse(3L, "Corte")));
    }

    @Test
    void criar_quandoBarbeiroValido_retornaBarbeiroComServicos() throws Exception {
        // Arrange
        when(barbeiroService.criar(request)).thenReturn(response);

        // Act
        var resultado = mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO));

        // Assert
        resultado.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.servicos[0].nome").value("Corte"));
        verify(barbeiroService).criar(request);
    }

    @Test
    void listar_quandoNomeEAtivosInformados_encaminhaFiltros() throws Exception {
        // Arrange
        when(barbeiroService.listar("Carlos", true)).thenReturn(List.of(response));

        // Act
        var resultado = mockMvc.perform(get(ROTA).param("nome", "Carlos").param("apenasAtivos", "true"));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Carlos"))
                .andExpect(jsonPath("$[0].ativo").value(true));
        verify(barbeiroService).listar("Carlos", true);
    }

    @Test
    void buscarPorId_quandoBarbeiroExiste_retornaBarbeiroEncontrado() throws Exception {
        // Arrange
        when(barbeiroService.buscarPorId(2L)).thenReturn(response);

        // Act
        var resultado = mockMvc.perform(get(ROTA + "/2"));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.nome").value("Carlos"));
        verify(barbeiroService).buscarPorId(2L);
    }

    @Test
    void servicosDoBarbeiro_quandoBarbeiroExiste_retornaServicosCompletos() throws Exception {
        // Arrange
        var servico = new ServicoResponse(3L, "Corte", new BigDecimal("45.00"), 30, true);
        when(barbeiroService.servicosDoBarbeiro(2L)).thenReturn(List.of(servico));

        // Act
        var resultado = mockMvc.perform(get(ROTA + "/2/servicos"));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valor").value(45.00))
                .andExpect(jsonPath("$[0].duracaoMinutos").value(30));
        verify(barbeiroService).servicosDoBarbeiro(2L);
    }

    @Test
    void atualizar_quandoCadastroValido_retornaBarbeiroAtualizado() throws Exception {
        // Arrange
        when(barbeiroService.atualizar(2L, request)).thenReturn(response);

        // Act
        var resultado = mockMvc.perform(put(ROTA + "/2")
                .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.servicos[0].id").value(3))
                .andExpect(jsonPath("$.ativo").value(true));
        verify(barbeiroService).atualizar(2L, request);
    }

    @Test
    void excluir_quandoBarbeiroExiste_retornaRespostaSemCorpo() throws Exception {
        // Arrange
        Long id = 2L;

        // Act
        var resultado = mockMvc.perform(delete(ROTA + "/{id}", id));

        // Assert
        resultado.andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(barbeiroService).excluir(id);
    }

    @Test
    void buscarPorId_quandoBarbeiroAusente_retornaApiError404() throws Exception {
        // Arrange
        String mensagem = "Barbeiro não encontrado com id 99";
        when(barbeiroService.buscarPorId(99L)).thenThrow(new ResourceNotFoundException(mensagem));

        // Act
        var resultado = mockMvc.perform(get(ROTA + "/99"));

        // Assert
        assertApiError(resultado, HttpStatus.NOT_FOUND, mensagem);
        verify(barbeiroService).buscarPorId(99L);
    }

    @Test
    void criar_quandoNomeVazio_retornaValidacaoSemChamarService() throws Exception {
        // Arrange
        String corpo = """
                {"nome": "", "servicoIds": [3], "ativo": true}
                """;

        // Act
        var resultado = mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(corpo));

        // Assert
        assertApiError(resultado, HttpStatus.BAD_REQUEST, "Erro de validação");
        resultado.andExpect(jsonPath("$.fields.nome").value("O nome é obrigatório"));
        verify(barbeiroService, never()).criar(any(BarbeiroRequest.class));
    }

    @Test
    void criar_quandoServiceLancaNegocioException_retornaApiError400() throws Exception {
        // Arrange
        String mensagem = "Regra de negócio violada";
        when(barbeiroService.criar(request)).thenThrow(new NegocioException(mensagem));

        // Act
        var resultado = mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO));

        // Assert
        assertApiError(resultado, HttpStatus.BAD_REQUEST, mensagem);
        verify(barbeiroService).criar(request);
    }
}
