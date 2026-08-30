package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.repository;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Enfermeiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnfermeiroRepository extends JpaRepository<Enfermeiro, Long> {
    List<Enfermeiro> findBySetorIgnoreCase(String setor);

    List<Enfermeiro> findByAtivoTrue();
}
