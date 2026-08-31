package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.controller;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.mapper.HospitalMapper;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.request.PacienteRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.response.PacienteResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.PacienteService;
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
@RequestMapping("/pacientes")
@Tag(name = "Pacientes", description = "Endpoints para gerenciamento de pacientes")
public class PacienteController {
    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @GetMapping
    @Operation(summary = "Lista todos os pacientes")
    @ApiResponse(responseCode = "200", description = "Pacientes listados com sucesso")
    public ResponseEntity<List<PacienteResponse>> buscarTodos() {
        List<PacienteResponse> pacientes = pacienteService.buscarTodos().stream()
                .map(HospitalMapper::toResponse)
                .toList();
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping("/filtro")
    @Operation(summary = "Filtra pacientes por sexo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pacientes filtrados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parametro invalido")
    })
    public ResponseEntity<List<PacienteResponse>> filtrarPorSexo(@RequestParam char sexo) {
        List<PacienteResponse> pacientes = pacienteService.filtrarPorSexo(sexo).stream()
                .map(HospitalMapper::toResponse)
                .toList();
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping("/ordenados-por-nome")
    @Operation(summary = "Lista pacientes ordenados por nome")
    @ApiResponse(responseCode = "200", description = "Pacientes ordenados com sucesso")
    public ResponseEntity<List<PacienteResponse>> listarOrdenadoPorNome() {
        List<PacienteResponse> pacientes = pacienteService.listarOrdenadoPorNome().stream()
                .map(HospitalMapper::toResponse)
                .toList();
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um paciente por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paciente encontrado"),
            @ApiResponse(responseCode = "404", description = "Paciente nao encontrado")
    })
    public ResponseEntity<PacienteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(HospitalMapper.toResponse(pacienteService.buscarPorId(id)));
    }

    @PostMapping
    @Operation(summary = "Cadastra um paciente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paciente cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos")
    })
    public ResponseEntity<PacienteResponse> inserir(@RequestBody @Valid PacienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(HospitalMapper.toResponse(pacienteService.cadastrar(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um paciente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paciente atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "404", description = "Paciente nao encontrado")
    })
    public ResponseEntity<PacienteResponse> atualizar(@PathVariable Long id, @RequestBody @Valid PacienteRequest request) {
        return ResponseEntity.ok(HospitalMapper.toResponse(pacienteService.atualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um paciente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Paciente removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Paciente nao encontrado")
    })
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        pacienteService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
