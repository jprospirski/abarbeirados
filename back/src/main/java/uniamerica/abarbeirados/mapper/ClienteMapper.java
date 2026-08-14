package uniamerica.abarbeirados.mapper;

import org.springframework.stereotype.Component;

import uniamerica.abarbeirados.dto.cliente.ClienteRequest;
import uniamerica.abarbeirados.dto.cliente.ClienteResponse;
import uniamerica.abarbeirados.model.Cliente;

@Component
public class ClienteMapper {
    
    public Cliente forEntity(ClienteRequest request) {
        return Cliente.builder()
                .nome(request.nome())
                .email(request.email())
                .telefone(request.telefone())
                .build();
    }

    public void updateEntity(ClienteRequest request, Cliente cliente) {
        cliente.setNome(request.nome());
        cliente.setEmail(request.email());
        cliente.setTelefone(request.telefone());
    }

    public ClienteResponse forResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getTelefone(),
                cliente.getDataCadastro()
        );
    }
}
