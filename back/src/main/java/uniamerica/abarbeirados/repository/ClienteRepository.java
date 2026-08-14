package uniamerica.abarbeirados.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import uniamerica.abarbeirados.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List <Cliente> findByNomeContainingIgnoreCase(String nome);
    Optional<Cliente> findByEmail(String email);
    Optional<Cliente> findByTelefone(String telefone);
}
