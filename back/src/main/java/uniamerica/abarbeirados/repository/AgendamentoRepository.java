package uniamerica.abarbeirados.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uniamerica.abarbeirados.model.Agendamento;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    // Filtro (busca/dia) e agrupamento por dia são feitos em memória no service por enquanto.
    // Se a base de dados crescer, migrar para @Query com filtros no banco.
}