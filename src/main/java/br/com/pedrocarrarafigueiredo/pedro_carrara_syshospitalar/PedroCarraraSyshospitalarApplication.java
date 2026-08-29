package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Atendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Enfermeiro;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Medico;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.TipoAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.AtendimentoService;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.EnfermeiroService;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.MedicoService;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.PacienteService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PedroCarraraSyshospitalarApplication  {

    public static void main(String[] args) {
        SpringApplication.run(PedroCarraraSyshospitalarApplication.class, args);
    }

}
