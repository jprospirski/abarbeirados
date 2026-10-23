package uniamerica.abarbeirados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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

import uniamerica.abarbeirados.model.Servico;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ServicoRepositoryTest {

    @Autowired
    private ServicoRepository servicoRepository;

    private Servico servicoAtivo;
    private Servico servicoInativo;
    private String termo;

    @BeforeEach
    void prepararServicos() {
        termo = "ServicoRepositorio " + UUID.randomUUID();
        servicoAtivo = servicoRepository.saveAndFlush(novoServico(termo + " Ativo", true));
        servicoInativo = servicoRepository.saveAndFlush(novoServico(termo + " Inativo", false));
    }

    @Test
    void findByNomeContainingIgnoreCase_quandoTrechoTemMaiusculas_retornaServicos() {
        // Arrange
        String busca = termo.toUpperCase(Locale.ROOT);

        // Act
        List<Servico> resultado = servicoRepository.findByNomeContainingIgnoreCase(busca);

        // Assert
        assertThat(resultado).containsExactlyInAnyOrder(servicoAtivo, servicoInativo);
    }

    @Test
    void findByNomeContainingIgnoreCase_quandoNomeNaoExiste_retornaListaVazia() {
        // Arrange
        String busca = "ausente-" + termo;

        // Act
        List<Servico> resultado = servicoRepository.findByNomeContainingIgnoreCase(busca);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void findByAtivo_quandoStatusInformado_retornaSomenteServicosCorrespondentes(boolean ativo) {
        // Arrange
        Servico esperado = ativo ? servicoAtivo : servicoInativo;
        Servico excluido = ativo ? servicoInativo : servicoAtivo;

        // Act
        List<Servico> resultado = servicoRepository.findByAtivo(ativo);

        // Assert
        assertThat(resultado).contains(esperado).doesNotContain(excluido);
    }

    private Servico novoServico(String nome, boolean ativo) {
        return Servico.builder().nome(nome).ativo(ativo)
                .valor(new BigDecimal("45.00")).duracaoMinutos(30).build();
    }
}
