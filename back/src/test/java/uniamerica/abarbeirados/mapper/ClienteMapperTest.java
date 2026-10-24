package uniamerica.abarbeirados.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import uniamerica.abarbeirados.dto.cliente.ClienteRequest;
import uniamerica.abarbeirados.dto.cliente.ClienteResponse;
import uniamerica.abarbeirados.model.Cliente;

class ClienteMapperTest {

    private final ClienteMapper mapper = new ClienteMapper();
    private Cliente cliente;

    @BeforeEach
    void prepararDados() {
        cliente = Cliente.builder().id(5L).nome("Ana").email("ana@exemplo.com")
                .telefone("45999999999").dataCadastro(LocalDateTime.of(2026, 10, 20, 9, 0)).build();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "ana@exemplo.com")
    void forEntity_quandoEmailInformadoOuOmitido_copiaDadosSemIdNemDataCadastro(String email) {
        // Arrange
        ClienteRequest request = new ClienteRequest("Ana", email, "45999999999");

        // Act
        Cliente resultado = mapper.forEntity(request);

        // Assert
        assertEquals(request.nome(), resultado.getNome());
        assertEquals(email, resultado.getEmail());
        assertEquals(request.telefone(), resultado.getTelefone());
        assertNull(resultado.getId());
        assertNull(resultado.getDataCadastro());
    }

    @Test
    void updateEntity_quandoDadosMudam_preservaIdEDataCadastro() {
        // Arrange
        ClienteRequest request = new ClienteRequest("Beatriz", null, "45988888888");
        LocalDateTime cadastroOriginal = cliente.getDataCadastro();

        // Act
        mapper.updateEntity(request, cliente);

        // Assert
        assertEquals(request.nome(), cliente.getNome());
        assertNull(cliente.getEmail());
        assertEquals(request.telefone(), cliente.getTelefone());
        assertEquals(5L, cliente.getId());
        assertEquals(cadastroOriginal, cliente.getDataCadastro());
    }

    @Test
    void forResponse_quandoClienteCompleto_retornaDadosComCadastro() {
        // Arrange
        ClienteResponse esperado = new ClienteResponse(5L, "Ana", "ana@exemplo.com",
                "45999999999", cliente.getDataCadastro());

        // Act
        ClienteResponse resultado = mapper.forResponse(cliente);

        // Assert
        assertEquals(esperado, resultado);
    }
}
