-- Schema inicial do Abarbeirados.
--
-- A partir daqui o Flyway e o dono do schema: o Hibernate roda em
-- ddl-auto=validate e so confere que as entidades batem com o que esta aqui.
-- Toda mudanca de tabela vira uma migration nova, nunca uma edicao desta.

CREATE TABLE clientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    telefone VARCHAR(255) NOT NULL,
    data_cadastro TIMESTAMP NOT NULL
);

CREATE TABLE servico (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    duracao_minutos INT NOT NULL,
    ativo BOOLEAN NOT NULL,
    valor DECIMAL(10,2) NOT NULL
);

CREATE TABLE barbeiro (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL
);

CREATE TABLE barbeiro_servico (
    barbeiro_id BIGINT NOT NULL REFERENCES barbeiro(id),
    servico_id BIGINT NOT NULL REFERENCES servico(id),
    PRIMARY KEY (barbeiro_id, servico_id)
);

CREATE TABLE agendamento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES clientes(id),
    servico_id BIGINT NOT NULL REFERENCES servico(id),
    barbeiro_id BIGINT NOT NULL REFERENCES barbeiro(id),
    data_hora TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    observacoes VARCHAR(255),
    valor DECIMAL(10,2) NOT NULL,
    duracao_minutos INT NOT NULL
);
