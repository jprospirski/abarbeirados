package uniamerica.abarbeirados.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import uniamerica.abarbeirados.model.Servico;
import uniamerica.abarbeirados.repository.ServicoRepository;

// O H2 roda em memoria e nasce vazio a cada boot; sem estas linhas o formulario
// de agendamento nao resolve nenhuma combinacao de servicos.
@Component
@RequiredArgsConstructor
public class ServicoSeeder implements CommandLineRunner {

    private final ServicoRepository servicoRepository;

    @Override
    public void run(String... args) {
        if (servicoRepository.count() > 0) {
            return;
        }

        servicoRepository.saveAll(List.of(
                servico("Corte", 40, "50.00"),
                servico("Barba", 20, "30.00"),
                servico("Sobrancelha", 20, "20.00"),
                servico("Química", 10, "0.00"),
                servico("Corte + Barba", 60, "70.00"),
                servico("Corte + Sobrancelha", 60, "60.00"),
                servico("Barba + Sobrancelha", 40, "40.00"),
                servico("Corte + Barba + Sobrancelha", 80, "85.00")));
    }

    private Servico servico(String nome, int duracaoMinutos, String valor) {
        return Servico.builder()
                .nome(nome)
                .duracaoMinutos(duracaoMinutos)
                .valor(new BigDecimal(valor))
                .build();
    }
}
