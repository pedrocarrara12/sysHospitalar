package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch.dto.PacienteCsvItem;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.repository.PacienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class PacienteImportacaoProcessor implements ItemProcessor<PacienteCsvItem, Paciente> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PacienteImportacaoProcessor.class);
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final Set<String> cpfsConhecidos = new HashSet<>();
    private final Set<String> emailsConhecidos = new HashSet<>();

    public PacienteImportacaoProcessor(PacienteRepository pacienteRepository) {
        pacienteRepository.findAll().forEach(paciente -> {
            cpfsConhecidos.add(paciente.getCpf());
            emailsConhecidos.add(paciente.getEmail().toLowerCase(Locale.ROOT));
        });
    }

    @Override
    public Paciente process(PacienteCsvItem item) {
        try {
            String nome = normalizarEspacos(item.nome());
            String cpf = somenteDigitos(item.cpf());
            LocalDate dataNascimento = LocalDate.parse(valorObrigatorio(item.dataNascimento()));
            String sexoTexto = valorObrigatorio(item.sexo()).toUpperCase(Locale.ROOT);
            String telefone = somenteDigitos(item.telefone());
            String email = valorObrigatorio(item.email()).toLowerCase(Locale.ROOT);
            boolean ativo = interpretarAtivo(item.ativo());

            if (nome.isBlank()) {
                throw new IllegalArgumentException("nome vazio");
            }
            if (!cpf.matches("\\d{11}")) {
                throw new IllegalArgumentException("CPF deve conter 11 digitos");
            }
            if (dataNascimento.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("data de nascimento futura");
            }
            if (!sexoTexto.matches("[MF]")) {
                throw new IllegalArgumentException("sexo deve ser M ou F");
            }
            if (telefone.isBlank()) {
                throw new IllegalArgumentException("telefone vazio");
            }
            if (!EMAIL.matcher(email).matches()) {
                throw new IllegalArgumentException("e-mail invalido");
            }
            if (cpfsConhecidos.contains(cpf)) {
                throw new IllegalArgumentException("CPF duplicado");
            }
            if (emailsConhecidos.contains(email)) {
                throw new IllegalArgumentException("e-mail duplicado");
            }

            cpfsConhecidos.add(cpf);
            emailsConhecidos.add(email);
            return new Paciente(nome, cpf, dataNascimento, sexoTexto.charAt(0), telefone, email, ativo);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            LOGGER.warn("Linha {} descartada da importacao: {}", item.numeroLinha(), exception.getMessage());
            return null;
        }
    }

    private String normalizarEspacos(String valor) {
        return valorObrigatorio(valor).replaceAll("\\s+", " ");
    }

    private String somenteDigitos(String valor) {
        return valorObrigatorio(valor).replaceAll("\\D", "");
    }

    private String valorObrigatorio(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("campo obrigatorio vazio");
        }
        return valor.trim();
    }

    private boolean interpretarAtivo(String valor) {
        if (valor == null || valor.isBlank()) {
            return true;
        }
        String normalizado = valor.trim().toLowerCase(Locale.ROOT);
        if (!normalizado.equals("true") && !normalizado.equals("false")) {
            throw new IllegalArgumentException("ativo deve ser true ou false");
        }
        return Boolean.parseBoolean(normalizado);
    }
}
