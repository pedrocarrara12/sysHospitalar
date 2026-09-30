INSERT INTO paciente (nome, cpf, data_nascimento, sexo, telefone, email, ativo)
VALUES ('Maria Silva', '12345678901', '1990-05-10', 'F', '65999990000', 'maria@email.com', true);

INSERT INTO medico (nome, idade, cpf, email, ativo, crm, especialidade)
VALUES ('Joao Medico', 40, '10987654321', 'joao@email.com', true, 'CRM123', 'Cardiologia');

INSERT INTO enfermeiro (nome, idade, cpf, email, ativo, coren, setor)
VALUES ('Ana Enfermeira', 32, '11122233344', 'ana@email.com', true, 'COREN123', 'UTI');
