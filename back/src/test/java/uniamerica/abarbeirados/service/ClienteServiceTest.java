package uniamerica.abarbeirados.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uniamerica.abarbeirados.dto.cliente.ClienteRequest;
import uniamerica.abarbeirados.dto.cliente.ClienteResponse;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.mapper.ClienteMapper;
import uniamerica.abarbeirados.model.Cliente;
import uniamerica.abarbeirados.repository.ClienteRepository;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    private static final Long CLIENTE_ID = 17L;
    private static final String CLIENTE_AUSENTE = "Cliente não encontrado com id 17";

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteMapper clienteMapper;

    @InjectMocks
    private ClienteService clienteService;

    @Captor
    private ArgumentCaptor<Cliente> clienteCaptor;

    private ClienteRequest request;
    private Cliente cliente;
    private ClienteResponse response;

    @BeforeEach
    void prepararDados() {
        LocalDateTime cadastro = LocalDateTime.of(2026, 5, 10, 9, 30);
        request = new ClienteRequest("Carlos", "carlos@example.com", "45999990000");
        cliente = Cliente.builder().id(CLIENTE_ID).nome(request.nome()).email(request.email())
                .telefone(request.telefone()).dataCadastro(cadastro).build();
        response = new ClienteResponse(CLIENTE_ID, request.nome(), request.email(), request.telefone(), cadastro);
    }

    @Test
    void criar_quandoDadosValidos_persisteClienteERetornaResponse() {
        // Arrange
        when(clienteMapper.forEntity(request)).thenReturn(cliente);
        when(clienteRepository.save(cliente)).thenReturn(cliente);
        when(clienteMapper.forResponse(cliente)).thenReturn(response);

        // Act
        ClienteResponse resultado = clienteService.criar(request);

        // Assert
        assertThat(resultado).isEqualTo(response);
        verify(clienteRepository).save(cliente);
        verify(clienteMapper).forEntity(request);
        verify(clienteMapper).forResponse(cliente);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void listar_quandoNomeNaoPreenchido_retornaTodosClientes(String nome) {
        // Arrange
        when(clienteRepository.findAll()).thenReturn(List.of(cliente));
        when(clienteMapper.forResponse(cliente)).thenReturn(response);

        // Act
        List<ClienteResponse> resultado = clienteService.listar(nome);

        // Assert
        assertThat(resultado).containsExactly(response);
        verify(clienteRepository).findAll();
        verify(clienteRepository, never()).findByNomeContainingIgnoreCase(any());
    }

    @Test
    void listar_quandoNomePreenchido_retornaClientesFiltrados() {
        // Arrange
        when(clienteRepository.findByNomeContainingIgnoreCase("Carlos")).thenReturn(List.of(cliente));
        when(clienteMapper.forResponse(cliente)).thenReturn(response);

        // Act
        List<ClienteResponse> resultado = clienteService.listar("Carlos");

        // Assert
        assertThat(resultado).containsExactly(response);
        verify(clienteRepository, never()).findAll();
        verify(clienteRepository).findByNomeContainingIgnoreCase("Carlos");
    }

    @Test
    void buscarPorId_quandoClienteExiste_retornaClienteConvertido() {
        // Arrange
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
        when(clienteMapper.forResponse(cliente)).thenReturn(response);

        // Act
        ClienteResponse resultado = clienteService.buscarPorId(CLIENTE_ID);

        // Assert
        assertThat(resultado).isEqualTo(response);
        verify(clienteRepository).findById(CLIENTE_ID);
    }

    @Test
    void buscarPorId_quandoClienteNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException erro = assertThrows(ResourceNotFoundException.class,
                () -> clienteService.buscarPorId(CLIENTE_ID));

        // Assert
        assertEquals(CLIENTE_AUSENTE, erro.getMessage());
        verificarQueNaoPersistiu();
    }

    @Test
    void atualizar_quandoClienteExiste_atualizaEntidadeEPersiste() {
        // Arrange
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(cliente)).thenReturn(cliente);
        when(clienteMapper.forResponse(cliente)).thenReturn(response);

        // Act
        ClienteResponse resultado = clienteService.atualizar(CLIENTE_ID, request);

        // Assert
        assertThat(resultado).isEqualTo(response);
        verify(clienteRepository).save(cliente);
        verify(clienteMapper).updateEntity(request, cliente);
    }

    @Test
    void atualizar_quandoClienteNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException erro = assertThrows(ResourceNotFoundException.class,
                () -> clienteService.atualizar(CLIENTE_ID, request));

        // Assert
        assertEquals(CLIENTE_AUSENTE, erro.getMessage());
        verificarQueNaoPersistiu();
    }

    @Test
    void excluir_quandoClienteExiste_removeClienteCarregado() {
        // Arrange
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));

        // Act
        clienteService.excluir(CLIENTE_ID);

        // Assert
        verify(clienteRepository).delete(clienteCaptor.capture());
        assertThat(clienteCaptor.getValue()).isSameAs(cliente);
        verifyNoInteractions(clienteMapper);
    }

    @Test
    void excluir_quandoClienteNaoExiste_lancaResourceNotFoundException() {
        // Arrange
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException erro = assertThrows(ResourceNotFoundException.class,
                () -> clienteService.excluir(CLIENTE_ID));

        // Assert
        assertEquals(CLIENTE_AUSENTE, erro.getMessage());
        verificarQueNaoPersistiu();
    }

    private void verificarQueNaoPersistiu() {
        verify(clienteRepository, never()).save(any(Cliente.class));
        verify(clienteRepository, never()).delete(any(Cliente.class));
        verifyNoInteractions(clienteMapper);
    }
}
