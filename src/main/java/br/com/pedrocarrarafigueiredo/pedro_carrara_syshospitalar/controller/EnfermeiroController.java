package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.controller;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Enfermeiro;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.EnfermeiroService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/enfermeiros")
public class EnfermeiroController {
    private final EnfermeiroService enfermeiroService;

    public EnfermeiroController(EnfermeiroService enfermeiroService) {
        this.enfermeiroService = enfermeiroService;
    }
    @GetMapping
    public ResponseEntity<List<Enfermeiro>> buscarTodosEnfermeiros() {
        List<Enfermeiro> enfermeiroList = enfermeiroService.buscarTodos();
        return ResponseEntity.ok(enfermeiroList);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Enfermeiro> buscarEnfermeiroPorId(@PathVariable Long id) {
        Enfermeiro enfermeiro = enfermeiroService.buscarPorId(id);
        return ResponseEntity.ok(enfermeiro);
    }

}
