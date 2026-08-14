package uniamerica.abarbeirados.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import uniamerica.abarbeirados.model.Servico;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<Servico> findByNomeContainingIgnoreCase(String nome);
    List<Servico> findByAtivo(Boolean ativo);
}
