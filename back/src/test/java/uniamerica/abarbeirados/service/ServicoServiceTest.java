package uniamerica.abarbeirados.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uniamerica.abarbeirados.dto.servico.ServicoRequest;
import uniamerica.abarbeirados.dto.servico.ServicoResponse;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.mapper.ServicoMapper;
import uniamerica.abarbeirados.model.Servico;
import uniamerica.abarbeirados.repository.ServicoRepository;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    private static final Long SERVICO_ID = 11L;
    private static final String SERVICO_AUSENTE = "Serviço não encontrado com id 11";

    @Mock
    private ServicoMapper servicoMapper;

    @Mock
    private ServicoRepository servicoRepository;

    @InjectMocks
    private ServicoService servicoService;

    @Captor
    private ArgumentCaptor<Servico> servicoCaptor;

    private Servico servico;
    private ServicoRequest request;
    private ServicoResponse response;

    @BeforeEach
    void prepararDados() {
        BigDecimal valor = new BigDecimal("35.00");
        request = new ServicoRequest("Barba", valor, 20, true);
        servico = Servico.builder().id(SERVICO_ID).nome(request.nome()).valor(valor)
                .duracaoMinutos(request.duracaoMinutos()).build();
        response = new ServicoResponse(SERVICO_ID, "Barba", valor, 20, true);
    }

    @Test
    void criar_quandoRequestValido_retornaServicoSalvo() {
        // Arrange
        when(servicoMapper.forEntity(request)).thenReturn(servico);
        when(servicoRepository.save(servico)).thenReturn(servico);
        when(servicoMapper.forResponse(servico)).thenReturn(response);

        // Act
        ServicoResponse resultado = servicoService.criar(request);

        // Assert
        assertThat(resultado).isEqualTo(response);
        verify(servicoMapper).forEntity(request);
        verify(servicoRepository).save(servico);
        verify(servicoMapper).forResponse(servico);
    }

    @ParameterizedTest
    @CsvSource(value = { "NULL,NULL", "'',false", "' ',NULL" }, nullValues = "NULL")
    void listar_quandoNomeAusenteEAtivosNaoSolicitados_retornaTodos(String nome, Boolean apenasAtivos) {
        // Arrange
        when(servicoRepository.findAll()).thenReturn(List.of(servico));
        when(servicoMapper.forResponse(servico)).thenReturn(response);

        // Act
        List<ServicoResponse> resultado = servicoService.listar(nome, apenasAtivos);

        // Assert
        assertThat(resultado).containsExactly(response);
        verify(servicoRepository, never()).findByAtivo(any());
        verify(servicoRepository).findAll();
        verify(servicoRepository, never()).findByNomeContainingIgnoreCase(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = { false, true })
    void listar_quandoNomeInformado_buscaPorNomeMesmoComFlagAtivos(Boolean apenasAtivos) {
        // Arrange
        when(servicoRepository.findByNomeContainingIgnoreCase("Barba")).thenReturn(List.of(servico));
        when(servicoMapper.forResponse(servico)).thenReturn(response);

        // Act
        List<ServicoResponse> resultado = servicoService.listar("Barba", apenasAtivos);

        // Assert
        assertThat(resultado).containsExactly(response);
        verify(servicoRepository, never()).findAll();
        verify(servicoRepository).findByNomeContainingIgnoreCase("Barba");
        verify(servicoRepository, never()).findByAtivo(any());
    }

    @Test
    void listar_quandoApenasAtivosTrueENomeBranco_retornaAtivos() {
        // Arrange
        when(servicoRepository.findByAtivo(true)).thenReturn(List.of(servico));
        when(servicoMapper.forResponse(servico)).thenReturn(response);

        // Act
        List<ServicoResponse> resultado = servicoService.listar(" ", true);

        // Assert
        assertThat(resultado).containsExactly(response);
        verify(servicoRepository, never()).findByNomeContainingIgnoreCase(any());
        verify(servicoRepository).findByAtivo(true);
        verify(servicoRepository, never()).findAll();
    }

    @Test
    void atualizar_quandoServicoExiste_delegaAlteracoesESalva() {
        // Arrange
        when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servico));
        when(servicoRepository.save(servico)).thenReturn(servico);
        when(servicoMapper.forResponse(servico)).thenReturn(response);

        // Act
        ServicoResponse resultado = servicoService.atualizar(SERVICO_ID, request);

        // Assert
        assertThat(resultado).isEqualTo(response);
        verify(servicoMapper).updateEntity(request, servico);
        verify(servicoRepository).save(servico);
    }

    @Test
    void atualizar_quandoServicoNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException erro = assertThrows(ResourceNotFoundException.class,
                () -> servicoService.atualizar(SERVICO_ID, request));

        // Assert
        assertEquals(SERVICO_AUSENTE, erro.getMessage());
        verificarQueNaoPersistiu();
    }

    @Test
    void buscarPorId_quandoServicoExiste_converteEntidadeEncontrada() {
        // Arrange
        when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servico));
        when(servicoMapper.forResponse(servico)).thenReturn(response);

        // Act
        ServicoResponse resultado = servicoService.buscarPorId(SERVICO_ID);

        // Assert
        assertThat(resultado).isEqualTo(response);
        verify(servicoMapper).forResponse(servico);
        verify(servicoRepository).findById(SERVICO_ID);
    }

    @Test
    void buscarPorId_quandoServicoNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException erro = assertThrows(ResourceNotFoundException.class,
                () -> servicoService.buscarPorId(SERVICO_ID));

        // Assert
        assertEquals(SERVICO_AUSENTE, erro.getMessage());
        verificarQueNaoPersistiu();
    }

    @Test
    void excluir_quandoServicoExiste_deletaServicoEncontrado() {
        // Arrange
        when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servico));

        // Act
        servicoService.excluir(SERVICO_ID);

        // Assert
        verify(servicoRepository).delete(servicoCaptor.capture());
        assertThat(servicoCaptor.getValue()).isSameAs(servico);
        verifyNoInteractions(servicoMapper);
    }

    @Test
    void excluir_quandoServicoNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException erro = assertThrows(ResourceNotFoundException.class,
                () -> servicoService.excluir(SERVICO_ID));

        // Assert
        assertEquals(SERVICO_AUSENTE, erro.getMessage());
        verificarQueNaoPersistiu();
    }

    private void verificarQueNaoPersistiu() {
        verify(servicoRepository, never()).save(any(Servico.class));
        verify(servicoRepository, never()).delete(any(Servico.class));
        verifyNoInteractions(servicoMapper);
    }
}
