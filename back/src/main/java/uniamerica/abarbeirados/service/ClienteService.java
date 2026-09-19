package uniamerica.abarbeirados.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uniamerica.abarbeirados.dto.cliente.ClienteRequest;
import uniamerica.abarbeirados.dto.cliente.ClienteResponse;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.mapper.ClienteMapper;
import uniamerica.abarbeirados.model.Cliente;
import uniamerica.abarbeirados.repository.ClienteRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        Cliente cliente = clienteMapper.forEntity(request);
        Cliente salvo = clienteRepository.save(cliente);

        log.info("Cliente {} criado: {} ({})", salvo.getId(), salvo.getNome(), salvo.getTelefone());

        return clienteMapper.forResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(String nome) {
        List<Cliente> clientes = (nome == null || nome.isBlank())
                ? clienteRepository.findAll()
                : clienteRepository.findByNomeContainingIgnoreCase(nome);

        return clientes.stream().map(clienteMapper::forResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return clienteMapper.forResponse(buscarEntidadePorId(id));
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarEntidadePorId(id);
        clienteMapper.updateEntity(request, cliente);
        clienteRepository.save(cliente);

        log.info("Cliente {} atualizado: {} ({})", cliente.getId(), cliente.getNome(), cliente.getTelefone());

        return clienteMapper.forResponse(cliente);
    }

    @Transactional
    public void excluir(Long id) {
        Cliente cliente = buscarEntidadePorId(id);
        clienteRepository.delete(cliente);

        log.info("Cliente {} excluído ({})", id, cliente.getNome());
    }

    private Cliente buscarEntidadePorId(Long id) {
        return clienteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id " + id));
    }
}