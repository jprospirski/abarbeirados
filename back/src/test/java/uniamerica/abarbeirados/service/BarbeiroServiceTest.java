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
import java.util.Set;

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

import uniamerica.abarbeirados.dto.barbeiro.BarbeiroRequest;
import uniamerica.abarbeirados.dto.barbeiro.BarbeiroResponse;
import uniamerica.abarbeirados.dto.barbeiro.ServicoResumoResponse;
import uniamerica.abarbeirados.dto.servico.ServicoResponse;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.mapper.BarbeiroMapper;
import uniamerica.abarbeirados.mapper.ServicoMapper;
import uniamerica.abarbeirados.model.Barbeiro;
import uniamerica.abarbeirados.model.Servico;
import uniamerica.abarbeirados.repository.BarbeiroRepository;
import uniamerica.abarbeirados.repository.ServicoRepository;

@ExtendWith(MockitoExtension.class)
class BarbeiroServiceTest {

    private static final Long BARBEIRO_ID = 7L;
    private static final Long SERVICO_ID = 3L;
    private static final String BARBEIRO_AUSENTE = "Barbeiro não encontrado com id 7";
    private static final String SERVICO_AUSENTE = "Serviço não encontrado com id 3";

    @Mock
    private BarbeiroRepository barbeiroRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private BarbeiroMapper barbeiroMapper;

    @Mock
    private ServicoMapper servicoMapper;

    @InjectMocks
    private BarbeiroService barbeiroService;

    @Captor
    private ArgumentCaptor<Barbeiro> barbeiroCaptor;

    private Servico servico;
    private Set<Servico> servicos;
    private Barbeiro barbeiro;
    private BarbeiroRequest request;
    private BarbeiroResponse response;

    @BeforeEach
    void prepararDados() {
        servico = Servico.builder().id(SERVICO_ID).nome("Corte")
                .valor(new BigDecimal("45.00")).duracaoMinutos(30).build();
        servicos = Set.of(servico);
        barbeiro = Barbeiro.builder().id(BARBEIRO_ID).nome("Bruno").servicos(servicos).build();
        request = new BarbeiroRequest("Bruno", List.of(SERVICO_ID), true);
        response = new BarbeiroResponse(BARBEIRO_ID, "Bruno", true,
                List.of(new ServicoResumoResponse(SERVICO_ID, "Corte")));
    }

    @Test
    void criar_quandoServicoExiste_salvaBarbeiroComServicoResolvido() {
        // Arrange
        when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servico));
        when(barbeiroMapper.forEntity(request, servicos)).thenReturn(barbeiro);
        when(barbeiroRepository.save(barbeiro)).thenReturn(barbeiro);
        when(barbeiroMapper.forResponse(barbeiro)).thenReturn(response);

        // Act
        BarbeiroResponse resultado = barbeiroService.criar(request);

        // Assert
        assertThat(resultado).isEqualTo(response);
        verify(barbeiroMapper).forEntity(request, servicos);
        verify(barbeiroRepository).save(barbeiro);
        verify(servicoRepository).findById(SERVICO_ID);
    }

    @Test
    void criar_quandoServicoNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException erro = assertThrows(ResourceNotFoundException.class,
                () -> barbeiroService.criar(request));

        // Assert
        assertEquals(SERVICO_AUSENTE, erro.getMessage());
        verificarQueNaoPersistiu();
    }

    @ParameterizedTest
    @CsvSource(value = { "NULL,NULL", "'',false", "' ',NULL" }, nullValues = "NULL")
    void listar_quandoNomeAusenteEAtivosNaoSolicitados_retornaTodos(String nome, Boolean apenasAtivos) {
        // Arrange
        when(barbeiroRepository.findAll()).thenReturn(List.of(barbeiro));
        when(barbeiroMapper.forResponse(barbeiro)).thenReturn(response);

        // Act
        List<BarbeiroResponse> resultado = barbeiroService.listar(nome, apenasAtivos);

        // Assert
        assertThat(resultado).containsExactly(response);
        verify(barbeiroRepository).findAll();
        verify(barbeiroRepository, never()).findByNomeContainingIgnoreCase(any());
        verify(barbeiroRepository, never()).findByAtivo(any());
    }

    @Test
    void listar_quandoNomeBrancoEApenasAtivosTrue_retornaSomenteAtivos() {
        // Arrange
        when(barbeiroRepository.findByAtivo(true)).thenReturn(List.of(barbeiro));
        when(barbeiroMapper.forResponse(barbeiro)).thenReturn(response);

        // Act
        List<BarbeiroResponse> resultado = barbeiroService.listar(" ", true);

        // Assert
        assertThat(resultado).containsExactly(response);
        verify(barbeiroRepository).findByAtivo(true);
        verify(barbeiroRepository, never()).findAll();
        verify(barbeiroRepository, never()).findByNomeContainingIgnoreCase(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = { false, true })
    void listar_quandoNomePreenchido_priorizaFiltroPorNome(Boolean apenasAtivos) {
        // Arrange
        when(barbeiroRepository.findByNomeContainingIgnoreCase("Bruno")).thenReturn(List.of(barbeiro));
        when(barbeiroMapper.forResponse(barbeiro)).thenReturn(response);

        // Act
        List<BarbeiroResponse> resultado = barbeiroService.listar("Bruno", apenasAtivos);

        // Assert
        assertThat(resultado).containsExactly(response);
        verify(barbeiroRepository).findByNomeContainingIgnoreCase("Bruno");
        verify(barbeiroRepository, never()).findByAtivo(any());
        verify(barbeiroRepository, never()).findAll();
    }

    @Test
    void buscarPorId_quandoBarbeiroExiste_retornaResponse() {
        // Arrange
        when(barbeiroRepository.findById(BARBEIRO_ID)).thenReturn(Optional.of(barbeiro));
        when(barbeiroMapper.forResponse(barbeiro)).thenReturn(response);

        // Act
        BarbeiroResponse resultado = barbeiroService.buscarPorId(BARBEIRO_ID);

        // Assert
        assertThat(resultado).isEqualTo(response);
        verify(barbeiroRepository).findById(BARBEIRO_ID);
        verify(barbeiroMapper).forResponse(barbeiro);
    }

    @Test
    void buscarPorId_quandoBarbeiroNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        when(barbeiroRepository.findById(BARBEIRO_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException erro = assertThrows(ResourceNotFoundException.class,
                () -> barbeiroService.buscarPorId(BARBEIRO_ID));

        // Assert
        assertEquals(BARBEIRO_AUSENTE, erro.getMessage());
        verificarQueNaoPersistiu();
        verifyNoInteractions(servicoRepository, servicoMapper);
    }

    @Test
    void servicosDoBarbeiro_quandoBarbeiroExiste_retornaServicosCompletos() {
        // Arrange
        ServicoResponse servicoResponse = new ServicoResponse(SERVICO_ID, "Corte",
                new BigDecimal("45.00"), 30, true);
        when(barbeiroRepository.findById(BARBEIRO_ID)).thenReturn(Optional.of(barbeiro));
        when(servicoMapper.forResponse(servico)).thenReturn(servicoResponse);

        // Act
        List<ServicoResponse> resultado = barbeiroService.servicosDoBarbeiro(BARBEIRO_ID);

        // Assert
        assertThat(resultado).containsExactly(servicoResponse);
        verify(servicoMapper).forResponse(servico);
        verifyNoInteractions(servicoRepository, barbeiroMapper);
    }

    @Test
    void servicosDoBarbeiro_quandoBarbeiroNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        when(barbeiroRepository.findById(BARBEIRO_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException erro = assertThrows(ResourceNotFoundException.class,
                () -> barbeiroService.servicosDoBarbeiro(BARBEIRO_ID));

        // Assert
        assertEquals(BARBEIRO_AUSENTE, erro.getMessage());
        verificarQueNaoPersistiu();
        verifyNoInteractions(servicoMapper);
    }

    @Test
    void atualizar_quandoBarbeiroEServicoExistem_atualizaESalva() {
        // Arrange
        when(barbeiroRepository.findById(BARBEIRO_ID)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servico));
        when(barbeiroRepository.save(barbeiro)).thenReturn(barbeiro);
        when(barbeiroMapper.forResponse(barbeiro)).thenReturn(response);

        // Act
        BarbeiroResponse resultado = barbeiroService.atualizar(BARBEIRO_ID, request);

        // Assert
        assertThat(resultado).isEqualTo(response);
        verify(barbeiroMapper).updateEntity(request, barbeiro, servicos);
        verify(barbeiroRepository).save(barbeiro);
        verify(servicoRepository).findById(SERVICO_ID);
    }

    @Test
    void atualizar_quandoBarbeiroNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        when(barbeiroRepository.findById(BARBEIRO_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException erro = assertThrows(ResourceNotFoundException.class,
                () -> barbeiroService.atualizar(BARBEIRO_ID, request));

        // Assert
        assertEquals(BARBEIRO_AUSENTE, erro.getMessage());
        verificarQueNaoPersistiu();
        verifyNoInteractions(servicoRepository);
    }

    @Test
    void atualizar_quandoServicoNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        when(barbeiroRepository.findById(BARBEIRO_ID)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException erro = assertThrows(ResourceNotFoundException.class,
                () -> barbeiroService.atualizar(BARBEIRO_ID, request));

        // Assert
        assertEquals(SERVICO_AUSENTE, erro.getMessage());
        verificarQueNaoPersistiu();
    }

    @Test
    void excluir_quandoBarbeiroExiste_removeEntidadeEncontrada() {
        // Arrange
        when(barbeiroRepository.findById(BARBEIRO_ID)).thenReturn(Optional.of(barbeiro));

        // Act
        barbeiroService.excluir(BARBEIRO_ID);

        // Assert
        verify(barbeiroRepository).delete(barbeiroCaptor.capture());
        assertThat(barbeiroCaptor.getValue()).isSameAs(barbeiro);
        verifyNoInteractions(barbeiroMapper, servicoRepository);
    }

    @Test
    void excluir_quandoBarbeiroNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        when(barbeiroRepository.findById(BARBEIRO_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException erro = assertThrows(ResourceNotFoundException.class,
                () -> barbeiroService.excluir(BARBEIRO_ID));

        // Assert
        assertEquals(BARBEIRO_AUSENTE, erro.getMessage());
        verificarQueNaoPersistiu();
    }

    private void verificarQueNaoPersistiu() {
        verify(barbeiroRepository, never()).save(any(Barbeiro.class));
        verify(barbeiroRepository, never()).delete(any(Barbeiro.class));
        verifyNoInteractions(barbeiroMapper);
    }
}
