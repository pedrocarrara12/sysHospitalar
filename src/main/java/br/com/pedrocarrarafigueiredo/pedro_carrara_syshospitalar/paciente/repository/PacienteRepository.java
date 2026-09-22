package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.repository;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.domain.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    List<Paciente> findBySexo(char sexo);

    List<Paciente> findAllByOrderByNomeAsc();
}
