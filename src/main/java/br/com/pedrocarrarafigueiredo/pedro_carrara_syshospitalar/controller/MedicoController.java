package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.controller;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.mapper.HospitalMapper;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.request.MedicoRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.response.MedicoResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.MedicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/medicos")
@Tag(name = "Médicos", description = "Endpoints para gerenciamento de médicos")
public class MedicoController {
    private final MedicoService medicoService;

    public MedicoController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }

    @GetMapping
    @Operation(summary = "Lista todos os médicos")
    @ApiResponse(responseCode = "200", description = "Medicos listados com sucesso")
    public ResponseEntity<List<MedicoResponse>> buscarTodos() {
        List<MedicoResponse> medicos = medicoService.buscarTodos().stream()
                .map(HospitalMapper::toResponse)
                .toList();
        return ResponseEntity.ok(medicos);
    }

    @GetMapping("/filtro")
    @Operation(summary = "Filtra médicos por especialidade")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medicos filtrados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parametro invalido")
    })
    public ResponseEntity<List<MedicoResponse>> filtrarPorEspecialidade(@RequestParam String especialidade) {
        List<MedicoResponse> medicos = medicoService.filtrarPorEspecialidade(especialidade).stream()
                .map(HospitalMapper::toResponse)
                .toList();
        return ResponseEntity.ok(medicos);
    }

    @GetMapping("/ativos")
    @Operation(summary = "Lista médicos ativos")
    @ApiResponse(responseCode = "200", description = "Medicos ativos listados com sucesso")
    public ResponseEntity<List<MedicoResponse>> listarAtivos() {
        List<MedicoResponse> medicos = medicoService.listarAtivos().stream()
                .map(HospitalMapper::toResponse)
                .toList();
        return ResponseEntity.ok(medicos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um médico por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medico encontrado"),
            @ApiResponse(responseCode = "404", description = "Medico nao encontrado")
    })
    public ResponseEntity<MedicoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(HospitalMapper.toResponse(medicoService.buscarPorId(id)));
    }

    @PostMapping
    @Operation(summary = "Cadastra um médico")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Medico cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos")
    })
    public ResponseEntity<MedicoResponse> inserir(@RequestBody @Valid MedicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(HospitalMapper.toResponse(medicoService.cadastrar(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um médico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medico atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "404", description = "Medico nao encontrado")
    })
    public ResponseEntity<MedicoResponse> atualizar(@PathVariable Long id, @RequestBody @Valid MedicoRequest request) {
        return ResponseEntity.ok(HospitalMapper.toResponse(medicoService.atualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um médico")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Medico removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Medico nao encontrado")
    })
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        medicoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
