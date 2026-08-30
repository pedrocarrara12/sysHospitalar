package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.repository;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Long> {
    List<Medico> findByEspecialidadeIgnoreCase(String especialidade);

    List<Medico> findByAtivoTrue();
}
