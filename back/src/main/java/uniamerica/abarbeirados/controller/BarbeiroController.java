package uniamerica.abarbeirados.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import uniamerica.abarbeirados.dto.barbeiro.BarbeiroRequest;
import uniamerica.abarbeirados.dto.barbeiro.BarbeiroResponse;
import uniamerica.abarbeirados.dto.servico.ServicoResponse;
import uniamerica.abarbeirados.service.BarbeiroService;

@RestController
@RequestMapping("/api/barbeiros")
@RequiredArgsConstructor
public class BarbeiroController {

    private final BarbeiroService barbeiroService;

    @PostMapping
    public ResponseEntity<BarbeiroResponse> criar(@Valid @RequestBody BarbeiroRequest request) {
        BarbeiroResponse response = barbeiroService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BarbeiroResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Boolean apenasAtivos) {
        return ResponseEntity.ok(barbeiroService.listar(nome, apenasAtivos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BarbeiroResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(barbeiroService.buscarPorId(id));
    }

    @GetMapping("/{id}/servicos")
    public ResponseEntity<List<ServicoResponse>> servicosDoBarbeiro(@PathVariable Long id) {
        return ResponseEntity.ok(barbeiroService.servicosDoBarbeiro(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BarbeiroResponse> atualizar(
            @PathVariable Long id, @Valid @RequestBody BarbeiroRequest request) {
        return ResponseEntity.ok(barbeiroService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        barbeiroService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
