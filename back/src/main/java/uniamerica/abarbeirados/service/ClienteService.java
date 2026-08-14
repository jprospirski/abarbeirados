package uniamerica.abarbeirados.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import uniamerica.abarbeirados.dto.cliente.ClienteRequest;
import uniamerica.abarbeirados.dto.cliente.ClienteResponse;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.mapper.ClienteMapper;
import uniamerica.abarbeirados.model.Cliente;
import uniamerica.abarbeirados.repository.ClienteRepository;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    public ClienteResponse criar(ClienteRequest request) {
        Cliente cliente = clienteMapper.forEntity(request);
        Cliente salvo = clienteRepository.save(cliente);
        return clienteMapper.forResponse(salvo);
    }

    public List<ClienteResponse> listar(String nome) {
        List<Cliente> clientes = (nome == null || nome.isBlank())
                ? clienteRepository.findAll()
                : clienteRepository.findByNomeContainingIgnoreCase(nome);

        return clientes.stream().map(clienteMapper::forResponse).toList();
    }

    public ClienteResponse buscarPorId(Long id) {
        return clienteMapper.forResponse(buscarEntidadePorId(id));
    }

    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarEntidadePorId(id);
        clienteMapper.updateEntity(request, cliente);
        return clienteMapper.forResponse(clienteRepository.save(cliente));
    }

    public void excluir(Long id) {
        Cliente cliente = buscarEntidadePorId(id);
        clienteRepository.delete(cliente);
    }

    private Cliente buscarEntidadePorId(Long id) {
        return clienteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id " + id));
    }
}