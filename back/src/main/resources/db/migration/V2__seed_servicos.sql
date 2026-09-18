-- Catalogo fixo de servicos. O formulario de agendamento traduz a combinacao
-- marcada no carrinho para uma destas linhas pelo NOME, entao mexer no texto
-- aqui quebra a resolucao na tela.
--
-- Substitui o antigo config/ServicoSeeder: com o Flyway dono do schema, manter
-- os dois duplicaria os dados a cada boot.

INSERT INTO servico (nome, duracao_minutos, ativo, valor) VALUES
('Corte', 40, true, 50.00),
('Barba', 20, true, 30.00),
('Sobrancelha', 20, true, 20.00),
('Química', 10, true, 0.00),
('Corte + Barba', 60, true, 70.00),
('Corte + Sobrancelha', 60, true, 60.00),
('Barba + Sobrancelha', 40, true, 40.00),
('Corte + Barba + Sobrancelha', 80, true, 85.00);
