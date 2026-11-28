package uniamerica.abarbeirados.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uniamerica.abarbeirados.dto.servico.ServicoRequest;
import uniamerica.abarbeirados.dto.servico.ServicoResponse;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.mapper.ServicoMapper;
import uniamerica.abarbeirados.model.Servico;
import uniamerica.abarbeirados.repository.ServicoRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final ServicoMapper servicoMapper;

    @Transactional
    public ServicoResponse criar(ServicoRequest request) {
        Servico servico = servicoMapper.forEntity(request);
        Servico salvo = servicoRepository.save(servico);

        log.info("Serviço {} criado: {} ({} min, R$ {})",
                salvo.getId(), salvo.getNome(), salvo.getDuracaoMinutos(), salvo.getValor());

        return servicoMapper.forResponse(salvo);
    }

    /** Lista com filtros opcionais de nome e de situação; os dois se combinam quando informados. */
    @Transactional(readOnly = true)
    public List<ServicoResponse> listar(String nome, Boolean apenasAtivos) {
        List<Servico> servicos = (nome != null && !nome.isBlank())
                ? servicoRepository.findByNomeContainingIgnoreCase(nome)
                : servicoRepository.findAll();

        return servicos.stream()
                .filter(servico -> !Boolean.TRUE.equals(apenasAtivos) || Boolean.TRUE.equals(servico.getAtivo()))
                .map(servicoMapper::forResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServicoResponse buscarPorId(Long id) {
        return servicoMapper.forResponse(buscarEntidadePorId(id));
    }

    @Transactional
    public ServicoResponse atualizar(Long id, ServicoRequest request) {
        Servico servico = buscarEntidadePorId(id);
        servicoMapper.updateEntity(request, servico);
        servicoRepository.save(servico);

        log.info("Serviço {} atualizado: {} ({} min, R$ {}, ativo={})",
                servico.getId(), servico.getNome(), servico.getDuracaoMinutos(), servico.getValor(), servico.getAtivo());

        return servicoMapper.forResponse(servico);
    }

    @Transactional
    public void excluir(Long id) {
        Servico servico = buscarEntidadePorId(id);
        servicoRepository.delete(servico);

        log.info("Serviço {} excluído ({})", id, servico.getNome());
    }

    private Servico buscarEntidadePorId(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com id " + id));
    }
}
