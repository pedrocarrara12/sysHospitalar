package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.controller;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Medico;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.MedicoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medicos")
public class MedicoController {
    private final MedicoService medicoService;

    public MedicoController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }
    @GetMapping
    public ResponseEntity<List<Medico>> buscarTodosMedicos() {
        List<Medico> medicoList = medicoService.buscarTodos();
        return ResponseEntity.ok(medicoList);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Medico> buscarPorId(@PathVariable Long id) {
        Medico medico = medicoService.buscarPorId(id);
        return ResponseEntity.ok(medico);
    }
    @PostMapping
    public ResponseEntity<Medico> criarMedico(@RequestBody Medico medico) {
        Medico medicoCadastro = medicoService.cadastrar(medico);

        return ResponseEntity.status(HttpStatus.CREATED).body(medicoCadastro);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Medico> atualizarMedico(@PathVariable Long id, Medico medico) {
        Medico medicoAtualizado = medicoService.atualizar(id,medico);
        return ResponseEntity.ok(medicoAtualizado);
    }

}
