package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch.dto.PacienteCsvItem;
import org.springframework.batch.infrastructure.item.file.LineMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;

public class PacienteCsvLineMapper implements LineMapper<PacienteCsvItem> {
    private final DelimitedLineTokenizer tokenizer;

    public PacienteCsvLineMapper() {
        tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_COMMA);
        tokenizer.setNames("nome", "cpf", "dataNascimento", "sexo", "telefone", "email", "ativo");
        tokenizer.setStrict(true);
    }

    @Override
    public PacienteCsvItem mapLine(String linha, int numeroLinha) {
        FieldSet campos = tokenizer.tokenize(linha);
        return new PacienteCsvItem(
                numeroLinha,
                campos.readString("nome"),
                campos.readString("cpf"),
                campos.readString("dataNascimento"),
                campos.readString("sexo"),
                campos.readString("telefone"),
                campos.readString("email"),
                campos.readString("ativo")
        );
    }
}
