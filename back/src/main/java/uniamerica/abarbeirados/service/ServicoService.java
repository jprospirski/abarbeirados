package uniamerica.abarbeirados.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import uniamerica.abarbeirados.dto.servico.ServicoRequest;
import uniamerica.abarbeirados.dto.servico.ServicoResponse;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.mapper.ServicoMapper;
import uniamerica.abarbeirados.model.Servico;
import uniamerica.abarbeirados.repository.ServicoRepository;

@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final ServicoMapper servicoMapper;

    public ServicoResponse criar(ServicoRequest request) {
        Servico servico = servicoMapper.forEntity(request);
        Servico salvo = servicoRepository.save(servico);
        return servicoMapper.forResponse(salvo);
    }

    public List<ServicoResponse> listar(String nome, Boolean apenasAtivos) {
        List<Servico> servico;
        if (nome != null && !nome.isBlank()) {
            servico = servicoRepository.findByNomeContainingIgnoreCase(nome);
        } else if (Boolean.TRUE.equals(apenasAtivos)) {
            servico = servicoRepository.findByAtivo(true);
        } else {
            servico = servicoRepository.findAll();
        }
        return servico.stream().map(servicoMapper::forResponse).toList();
    }

    public ServicoResponse buscarPorId(Long id) {
        return servicoMapper.forResponse(buscarEntidadePorId(id));
    }

    public ServicoResponse atualizar(Long id, ServicoRequest request) {
        Servico servico = buscarEntidadePorId(id);
        servicoMapper.updateEntity(request, servico);
        return servicoMapper.forResponse(servicoRepository.save(servico));
    }

    public void excluir(Long id) {
        Servico servico = buscarEntidadePorId(id);
        servicoRepository.delete(servico);
    }

    private Servico buscarEntidadePorId(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com id " + id));
    }
}