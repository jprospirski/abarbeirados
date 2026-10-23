package uniamerica.abarbeirados.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uniamerica.abarbeirados.dto.agendamento.AgendamentoRequest;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoResponse;
import uniamerica.abarbeirados.dto.agendamento.AtualizarStatusRequest;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.mapper.AgendamentoMapper;
import uniamerica.abarbeirados.model.Agendamento;
import uniamerica.abarbeirados.model.Barbeiro;
import uniamerica.abarbeirados.model.Cliente;
import uniamerica.abarbeirados.model.Servico;
import uniamerica.abarbeirados.model.StatusAgendamento;
import uniamerica.abarbeirados.repository.AgendamentoRepository;
import uniamerica.abarbeirados.repository.BarbeiroRepository;
import uniamerica.abarbeirados.repository.ClienteRepository;
import uniamerica.abarbeirados.repository.ServicoRepository;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    private static final Long AGENDAMENTO_ID = 10L;
    private static final Long CLIENTE_ID = 1L;
    private static final Long BARBEIRO_ID = 2L;
    private static final Long SERVICO_ID = 3L;
    private static final LocalDateTime DATA_HORA = LocalDateTime.of(2030, 5, 20, 10, 0);
    private static final String AGENDAMENTO_NAO_ENCONTRADO = "Agendamento não encontrado com id 10";
    private static final String CLIENTE_NAO_ENCONTRADO = "Cliente não encontrado com id 1";
    private static final String BARBEIRO_NAO_ENCONTRADO = "Barbeiro não encontrado com id 2";
    private static final String SERVICO_NAO_ENCONTRADO = "Serviço não encontrado com id 3";

    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private BarbeiroRepository barbeiroRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private AgendamentoMapper agendamentoMapper;
    @InjectMocks
    private AgendamentoService service;

    private Cliente cliente;
    private Barbeiro barbeiro;
    private Servico servico;
    private Agendamento agendamento;
    private AgendamentoRequest request;
    private AgendamentoResponse response;

    @BeforeEach
    void prepararDados() {
        cliente = Cliente.builder().id(CLIENTE_ID).nome("Ana").telefone("45999990000").build();
        barbeiro = Barbeiro.builder().id(BARBEIRO_ID).nome("Pedro").build();
        servico = Servico.builder().id(SERVICO_ID).nome("Corte")
                .valor(new BigDecimal("40.00")).duracaoMinutos(30).build();
        agendamento = agendamento(AGENDAMENTO_ID, DATA_HORA);
        request = new AgendamentoRequest(CLIENTE_ID, BARBEIRO_ID, SERVICO_ID, DATA_HORA, "Sem perfume");
        response = resposta(agendamento);
    }

    @Test
    void criar_quandoReferenciasExistem_salvaERetornaAgendamento() {
        // Arrange
        configurarReferencias();
        Agendamento novo = agendamento(null, DATA_HORA);
        when(agendamentoMapper.forEntity(request, cliente, barbeiro, servico)).thenReturn(novo);
        when(agendamentoRepository.save(novo)).thenReturn(agendamento);
        when(agendamentoMapper.forResponse(agendamento)).thenReturn(response);

        // Act
        AgendamentoResponse resultado = service.criar(request);

        // Assert
        assertEquals(response, resultado);
        verify(agendamentoMapper).forEntity(request, cliente, barbeiro, servico);
        verify(agendamentoRepository).save(novo);
    }

    @Test
    void criar_quandoClienteNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.criar(request));

        // Assert
        assertEquals(CLIENTE_NAO_ENCONTRADO, exception.getMessage());
        verifyNoInteractions(barbeiroRepository, servicoRepository, agendamentoMapper);
        verificarSemPersistencia();
    }

    @Test
    void criar_quandoBarbeiroNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(BARBEIRO_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.criar(request));

        // Assert
        assertEquals(BARBEIRO_NAO_ENCONTRADO, exception.getMessage());
        verifyNoInteractions(servicoRepository, agendamentoMapper);
        verificarSemPersistencia();
    }

    @Test
    void criar_quandoServicoNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(BARBEIRO_ID)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.criar(request));

        // Assert
        assertEquals(SERVICO_NAO_ENCONTRADO, exception.getMessage());
        verifyNoInteractions(agendamentoMapper);
        verificarSemPersistencia();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void listar_quandoBuscaNaoInformada_retornaAgendamentos(String busca) {
        // Arrange
        when(agendamentoRepository.findAll()).thenReturn(List.of(agendamento));
        when(agendamentoMapper.forResponse(agendamento)).thenReturn(response);

        // Act
        List<AgendamentoResponse> resultado = service.listar(busca, null);

        // Assert
        assertEquals(List.of(response), resultado);
        verify(agendamentoRepository).findAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"aNa", "pEdRo", "cOrTe"})
    void listar_quandoBuscaCorrespondeAoClienteBarbeiroOuServico_retornaAgendamento(String busca) {
        // Arrange
        when(agendamentoRepository.findAll()).thenReturn(List.of(agendamento));
        when(agendamentoMapper.forResponse(agendamento)).thenReturn(response);

        // Act
        List<AgendamentoResponse> resultado = service.listar(busca, null);

        // Assert
        assertEquals(List.of(response), resultado);
        verify(agendamentoMapper).forResponse(agendamento);
    }

    @Test
    void listar_quandoBuscaNaoCorrespondeAosNomes_retornaListaVazia() {
        // Arrange
        when(agendamentoRepository.findAll()).thenReturn(List.of(agendamento));

        // Act
        List<AgendamentoResponse> resultado = service.listar("inexistente", null);

        // Assert
        assertEquals(List.of(), resultado);
        verifyNoInteractions(agendamentoMapper);
    }

    @Test
    void listar_quandoHorariosDesordenados_retornaOrdemCronologica() {
        // Arrange
        Agendamento anterior = agendamento(11L, DATA_HORA.minusHours(1));
        AgendamentoResponse respostaAnterior = resposta(anterior);
        when(agendamentoRepository.findAll()).thenReturn(List.of(agendamento, anterior));
        when(agendamentoMapper.forResponse(agendamento)).thenReturn(response);
        when(agendamentoMapper.forResponse(anterior)).thenReturn(respostaAnterior);

        // Act
        List<AgendamentoResponse> resultado = service.listar(null, null);

        // Assert
        assertEquals(List.of(respostaAnterior, response), resultado);
        verify(agendamentoRepository).findAll();
    }

    @Test
    void buscarPorId_quandoAgendamentoExiste_retornaResposta() {
        // Arrange
        configurarAgendamentoEncontrado();
        when(agendamentoMapper.forResponse(agendamento)).thenReturn(response);

        // Act
        AgendamentoResponse resultado = service.buscarPorId(AGENDAMENTO_ID);

        // Assert
        assertEquals(response, resultado);
        verify(agendamentoRepository).findById(AGENDAMENTO_ID);
    }

    @Test
    void buscarPorId_quandoAgendamentoNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        configurarAgendamentoNaoEncontrado();

        // Act
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.buscarPorId(AGENDAMENTO_ID));

        // Assert
        assertEquals(AGENDAMENTO_NAO_ENCONTRADO, exception.getMessage());
        verifyNoInteractions(agendamentoMapper);
        verificarSemPersistencia();
    }

    @Test
    void atualizar_quandoAgendamentoEReferenciasExistem_atualizaESalva() {
        // Arrange
        configurarAgendamentoEncontrado();
        configurarReferencias();
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);
        when(agendamentoMapper.forResponse(agendamento)).thenReturn(response);

        // Act
        AgendamentoResponse resultado = service.atualizar(AGENDAMENTO_ID, request);

        // Assert
        assertEquals(response, resultado);
        verify(agendamentoMapper).updateEntity(request, agendamento, cliente, barbeiro, servico);
        verify(agendamentoRepository).save(agendamento);
    }

    @Test
    void atualizar_quandoAgendamentoNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        configurarAgendamentoNaoEncontrado();

        // Act
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.atualizar(AGENDAMENTO_ID, request));

        // Assert
        assertEquals(AGENDAMENTO_NAO_ENCONTRADO, exception.getMessage());
        verifyNoInteractions(clienteRepository, barbeiroRepository, servicoRepository, agendamentoMapper);
        verificarSemPersistencia();
    }

    @Test
    void atualizar_quandoClienteNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        configurarAgendamentoEncontrado();
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.atualizar(AGENDAMENTO_ID, request));

        // Assert
        assertEquals(CLIENTE_NAO_ENCONTRADO, exception.getMessage());
        verifyNoInteractions(barbeiroRepository, servicoRepository, agendamentoMapper);
        verificarSemPersistencia();
    }

    @Test
    void atualizar_quandoBarbeiroNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        configurarAgendamentoEncontrado();
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(BARBEIRO_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.atualizar(AGENDAMENTO_ID, request));

        // Assert
        assertEquals(BARBEIRO_NAO_ENCONTRADO, exception.getMessage());
        verifyNoInteractions(servicoRepository, agendamentoMapper);
        verificarSemPersistencia();
    }

    @Test
    void atualizar_quandoServicoNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        configurarAgendamentoEncontrado();
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(BARBEIRO_ID)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.atualizar(AGENDAMENTO_ID, request));

        // Assert
        assertEquals(SERVICO_NAO_ENCONTRADO, exception.getMessage());
        verifyNoInteractions(agendamentoMapper);
        verificarSemPersistencia();
    }

    @ParameterizedTest
    @EnumSource(StatusAgendamento.class)
    void atualizarStatus_quandoAgendamentoExiste_salvaStatusSolicitado(StatusAgendamento status) {
        // Arrange
        configurarAgendamentoEncontrado();
        AtualizarStatusRequest statusRequest = new AtualizarStatusRequest(status);
        ArgumentCaptor<Agendamento> captor = ArgumentCaptor.forClass(Agendamento.class);
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);
        when(agendamentoMapper.forResponse(agendamento)).thenAnswer(invocation -> resposta(agendamento));

        // Act
        AgendamentoResponse resultado = service.atualizarStatus(AGENDAMENTO_ID, statusRequest);

        // Assert
        verify(agendamentoRepository).save(captor.capture());
        assertEquals(status, captor.getValue().getStatus());
        assertEquals(status, resultado.status());
        verify(agendamentoMapper).forResponse(agendamento);
    }

    @Test
    void atualizarStatus_quandoAgendamentoNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        configurarAgendamentoNaoEncontrado();
        AtualizarStatusRequest statusRequest = new AtualizarStatusRequest(StatusAgendamento.CONFIRMADO);

        // Act
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.atualizarStatus(AGENDAMENTO_ID, statusRequest));

        // Assert
        assertEquals(AGENDAMENTO_NAO_ENCONTRADO, exception.getMessage());
        verifyNoInteractions(agendamentoMapper);
        verificarSemPersistencia();
    }

    @Test
    void excluir_quandoAgendamentoExiste_excluiEntidadeEncontrada() {
        // Arrange
        configurarAgendamentoEncontrado();
        ArgumentCaptor<Agendamento> captor = ArgumentCaptor.forClass(Agendamento.class);

        // Act
        service.excluir(AGENDAMENTO_ID);

        // Assert
        verify(agendamentoRepository).delete(captor.capture());
        assertEquals(agendamento, captor.getValue());
        verify(agendamentoRepository, never()).save(any(Agendamento.class));
    }

    @Test
    void excluir_quandoAgendamentoNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        configurarAgendamentoNaoEncontrado();

        // Act
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.excluir(AGENDAMENTO_ID));

        // Assert
        assertEquals(AGENDAMENTO_NAO_ENCONTRADO, exception.getMessage());
        verificarSemPersistencia();
    }

    private void configurarReferencias() {
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(BARBEIRO_ID)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servico));
    }

    private void configurarAgendamentoEncontrado() {
        when(agendamentoRepository.findById(AGENDAMENTO_ID)).thenReturn(Optional.of(agendamento));
    }

    private void configurarAgendamentoNaoEncontrado() {
        when(agendamentoRepository.findById(AGENDAMENTO_ID)).thenReturn(Optional.empty());
    }

    private void verificarSemPersistencia() {
        verify(agendamentoRepository, never()).save(any(Agendamento.class));
        verify(agendamentoRepository, never()).delete(any(Agendamento.class));
    }

    private Agendamento agendamento(Long id, LocalDateTime dataHora) {
        return Agendamento.builder().id(id).cliente(cliente).barbeiro(barbeiro).servico(servico)
                .dataHora(dataHora).status(StatusAgendamento.AGENDADO).observacoes("Sem perfume")
                .valor(servico.getValor()).duracaoMinutos(servico.getDuracaoMinutos()).build();
    }

    private AgendamentoResponse resposta(Agendamento entidade) {
        return new AgendamentoResponse(entidade.getId(), cliente.getId(), cliente.getNome(), cliente.getTelefone(),
                barbeiro.getId(), barbeiro.getNome(), servico.getId(), servico.getNome(), entidade.getValor(),
                entidade.getDuracaoMinutos(), entidade.getDataHora(), entidade.getStatus(), entidade.getObservacoes());
    }
}
