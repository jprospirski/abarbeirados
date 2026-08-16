package uniamerica.abarbeirados.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uniamerica.abarbeirados.entity.Agendamento;
import uniamerica.abarbeirados.entity.StatusAgendamento;
import uniamerica.abarbeirados.service.AgendamentoService;

import java.util.List;

@RestController
@RequestMapping("/agendamento")
public class AgendamentoController {
    @Autowired
    private AgendamentoService agendamentoService;

    @PostMapping
    public ResponseEntity<Agendamento> save(@Valid @RequestBody Agendamento agendamento) {
        Agendamento save = agendamentoService.save(agendamento);
        return ResponseEntity.status(HttpStatus.CREATED).body(save);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Agendamento> buscarPorId(@PathVariable Long id) {
        Agendamento agendamento = agendamentoService.findById(id);
        if (agendamento == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(agendamento);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Agendamento>> findAllByStatus(@PathVariable StatusAgendamento status) {
        return ResponseEntity.ok(agendamentoService.findAllByStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Agendamento> update(@PathVariable Long id, @Valid @RequestBody Agendamento agendamento) {
        Agendamento updated = agendamentoService.updateAgendamento(id, agendamento);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Agendamento> delet(@PathVariable Long id) {
        boolean deleted = agendamentoService.deleteById(id);
        if (deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/status/{status}")
    public ResponseEntity<Void> deleteByidAndStatus(@PathVariable Long id, @PathVariable StatusAgendamento status) {
        boolean deleted = agendamentoService.deleteByIdAndStatus(id, status);
        if (deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}



