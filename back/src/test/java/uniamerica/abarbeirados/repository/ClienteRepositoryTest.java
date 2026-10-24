package uniamerica.abarbeirados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import uniamerica.abarbeirados.model.Cliente;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;

    private Cliente cliente;
    private String termo;

    @BeforeEach
    void prepararCliente() {
        UUID identificador = UUID.randomUUID();
        termo = identificador.toString();
        cliente = clienteRepository.saveAndFlush(novoCliente(identificador));
    }

    @Test
    void findByNomeContainingIgnoreCase_quandoTrechoTemMaiusculas_retornaCliente() {
        // Arrange
        String busca = termo.toUpperCase(Locale.ROOT);

        // Act
        List<Cliente> resultado = clienteRepository.findByNomeContainingIgnoreCase(busca);

        // Assert
        assertThat(resultado).containsExactly(cliente);
    }

    @Test
    void findByNomeContainingIgnoreCase_quandoNomeNaoExiste_retornaListaVazia() {
        // Arrange
        String busca = "ausente-" + termo;

        // Act
        List<Cliente> resultado = clienteRepository.findByNomeContainingIgnoreCase(busca);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void findByEmail_quandoEmailExiste_retornaCliente() {
        // Arrange
        String email = cliente.getEmail();

        // Act
        Optional<Cliente> resultado = clienteRepository.findByEmail(email);

        // Assert
        assertThat(resultado).contains(cliente);
    }

    @Test
    void findByEmail_quandoEmailNaoExiste_retornaOptionalVazio() {
        // Arrange
        String email = "ausente-" + cliente.getEmail();

        // Act
        Optional<Cliente> resultado = clienteRepository.findByEmail(email);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void findByTelefone_quandoTelefoneExiste_retornaCliente() {
        // Arrange
        String telefone = cliente.getTelefone();

        // Act
        Optional<Cliente> resultado = clienteRepository.findByTelefone(telefone);

        // Assert
        assertThat(resultado).contains(cliente);
    }

    @Test
    void findByTelefone_quandoTelefoneNaoExiste_retornaOptionalVazio() {
        // Arrange
        String telefone = "0" + cliente.getTelefone();

        // Act
        Optional<Cliente> resultado = clienteRepository.findByTelefone(telefone);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void saveAndFlush_quandoDataCadastroNaoInformada_preencheDataCadastro() {
        // Arrange
        Cliente novo = novoCliente(UUID.randomUUID());
        LocalDateTime antes = LocalDateTime.now();

        // Act
        Cliente salvo = clienteRepository.saveAndFlush(novo);
        LocalDateTime depois = LocalDateTime.now();

        // Assert
        assertThat(salvo.getDataCadastro()).isNotNull().isBetween(antes, depois);
        assertThat(salvo.getId()).isNotNull();
    }

    private Cliente novoCliente(UUID identificador) {
        return Cliente.builder()
                .nome("ClienteRepositorio " + identificador)
                .email(identificador + "@example.com")
                .telefone(Long.toUnsignedString(identificador.getMostSignificantBits()))
                .build();
    }
}
