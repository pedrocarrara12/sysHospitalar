package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.repository;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.domain.Atendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.TipoAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {
    List<Atendimento> findByStatusAtendimento(StatusAtendimento statusAtendimento);

    List<Atendimento> findByTipoAtendimento(TipoAtendimento tipoAtendimento);

    List<Atendimento> findAllByOrderByDataHoraAtendimentoAsc();
}
