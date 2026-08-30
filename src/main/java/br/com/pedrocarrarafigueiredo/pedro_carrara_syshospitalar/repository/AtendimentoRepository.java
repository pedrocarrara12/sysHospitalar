package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.repository;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Atendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.TipoAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {
    List<Atendimento> findByStatusAtendimento(StatusAtendimento statusAtendimento);

    List<Atendimento> findByTipoAtendimento(TipoAtendimento tipoAtendimento);

    List<Atendimento> findAllByOrderByDataHoraAtendimentoAsc();
}
