package uniamerica.abarbeirados.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import uniamerica.abarbeirados.dto.barbeiro.BarbeiroRequest;
import uniamerica.abarbeirados.dto.barbeiro.BarbeiroResponse;
import uniamerica.abarbeirados.dto.servico.ServicoResponse;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.mapper.BarbeiroMapper;
import uniamerica.abarbeirados.mapper.ServicoMapper;
import uniamerica.abarbeirados.model.Barbeiro;
import uniamerica.abarbeirados.model.Servico;
import uniamerica.abarbeirados.repository.BarbeiroRepository;
import uniamerica.abarbeirados.repository.ServicoRepository;

@Service
@RequiredArgsConstructor
public class BarbeiroService {

    private final BarbeiroRepository barbeiroRepository;
    private final ServicoRepository servicoRepository;
    private final BarbeiroMapper barbeiroMapper;
    private final ServicoMapper servicoMapper;

    @Transactional
    public BarbeiroResponse criar(BarbeiroRequest request) {
        Set<Servico> servicos = buscarServicos(request.servicoIds());

        Barbeiro barbeiro = barbeiroMapper.forEntity(request, servicos);
        return barbeiroMapper.forResponse(barbeiroRepository.save(barbeiro));
    }

    /** nome e apenasAtivos combinam entre si, nao sao mutuamente exclusivos. */
    @Transactional(readOnly = true)
    public List<BarbeiroResponse> listar(String nome, Boolean apenasAtivos) {
        List<Barbeiro> barbeiros = (nome != null && !nome.isBlank())
                ? barbeiroRepository.findByNomeContainingIgnoreCase(nome)
                : barbeiroRepository.findAll();

        return barbeiros.stream()
                .filter(barbeiro -> !Boolean.TRUE.equals(apenasAtivos) || Boolean.TRUE.equals(barbeiro.getAtivo()))
                .map(barbeiroMapper::forResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BarbeiroResponse buscarPorId(Long id) {
        return barbeiroMapper.forResponse(buscarEntidadePorId(id));
    }

    /** Os serviços que o barbeiro atende, na forma completa de /api/servicos. */
    @Transactional(readOnly = true)
    public List<ServicoResponse> servicosDoBarbeiro(Long id) {
        Barbeiro barbeiro = buscarEntidadePorId(id);
        return barbeiro.getServicos().stream().map(servicoMapper::forResponse).toList();
    }

    @Transactional
    public BarbeiroResponse atualizar(Long id, BarbeiroRequest request) {
        Barbeiro barbeiro = buscarEntidadePorId(id);
        Set<Servico> servicos = buscarServicos(request.servicoIds());

        barbeiroMapper.updateEntity(request, barbeiro, servicos);
        return barbeiroMapper.forResponse(barbeiroRepository.save(barbeiro));
    }

    @Transactional
    public void excluir(Long id) {
        Barbeiro barbeiro = buscarEntidadePorId(id);
        barbeiroRepository.delete(barbeiro);
    }

    private Barbeiro buscarEntidadePorId(Long id) {
        return barbeiroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado com id " + id));
    }

    /*
     * Um id que nao existe derruba o cadastro inteiro em vez de salvar o barbeiro
     * com menos servicos do que a tela pediu, sem avisar ninguem.
     */
    private Set<Servico> buscarServicos(List<Long> ids) {
        Set<Servico> servicos = new HashSet<>();

        for (Long id : ids) {
            Servico servico = servicoRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com id " + id));
            servicos.add(servico);
        }

        return servicos;
    }
}
