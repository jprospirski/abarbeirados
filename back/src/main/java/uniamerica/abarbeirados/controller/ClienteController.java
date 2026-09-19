package uniamerica.abarbeirados.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import uniamerica.abarbeirados.dto.cep.CepResponse;
import uniamerica.abarbeirados.dto.cliente.ClienteRequest;
import uniamerica.abarbeirados.dto.cliente.ClienteResponse;
import uniamerica.abarbeirados.service.CepService;
import uniamerica.abarbeirados.service.ClienteService;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final CepService cepService;

    @PostMapping
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse response = clienteService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listar(
            @RequestParam(required = false) String nome) {
        return ResponseEntity.ok(clienteService.listar(nome));
    }

    /**
     * Consulta de CEP na ViaCEP, usada pelo formulario de cliente para preencher
     * o endereco. E so consulta: nada de endereco e gravado na tabela clientes.
     */
    @GetMapping("/cep/{cep}")
    public ResponseEntity<CepResponse> buscarCep(@PathVariable String cep) {
        return ResponseEntity.ok(cepService.buscar(cep));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> atualizar(
            @PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        clienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
