package uniamerica.abarbeirados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import uniamerica.abarbeirados.model.Barbeiro;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BarbeiroRepositoryTest {

    @Autowired
    private BarbeiroRepository barbeiroRepository;

    private Barbeiro barbeiroAtivo;
    private Barbeiro barbeiroInativo;
    private String termo;

    @BeforeEach
    void prepararBarbeiros() {
        termo = "BarbeiroRepositorio " + UUID.randomUUID();
        barbeiroAtivo = barbeiroRepository.saveAndFlush(
                Barbeiro.builder().nome(termo + " Ativo").ativo(true).build());
        barbeiroInativo = barbeiroRepository.saveAndFlush(
                Barbeiro.builder().nome(termo + " Inativo").ativo(false).build());
    }

    @Test
    void findByNomeContainingIgnoreCase_quandoTrechoTemMaiusculas_retornaBarbeiros() {
        // Arrange
        String busca = termo.toUpperCase(Locale.ROOT);

        // Act
        List<Barbeiro> resultado = barbeiroRepository.findByNomeContainingIgnoreCase(busca);

        // Assert
        assertThat(resultado).containsExactlyInAnyOrder(barbeiroAtivo, barbeiroInativo);
    }

    @Test
    void findByNomeContainingIgnoreCase_quandoNomeNaoExiste_retornaListaVazia() {
        // Arrange
        String busca = "ausente-" + termo;

        // Act
        List<Barbeiro> resultado = barbeiroRepository.findByNomeContainingIgnoreCase(busca);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void findByAtivo_quandoStatusInformado_retornaSomenteBarbeirosCorrespondentes(boolean ativo) {
        // Arrange
        Barbeiro esperado = ativo ? barbeiroAtivo : barbeiroInativo;
        Barbeiro excluido = ativo ? barbeiroInativo : barbeiroAtivo;

        // Act
        List<Barbeiro> resultado = barbeiroRepository.findByAtivo(ativo);

        // Assert
        assertThat(resultado).contains(esperado).doesNotContain(excluido);
    }
}
