-- SEED DE DEMONSTRAÇÃO — dados fictícios.
-- Existe para o painel ter o que mostrar antes de os cadastros ficarem prontos.
-- REMOVER por uma migration futura antes de entrar em produção com dados reais.

INSERT INTO usuario (id, nome, email, senha_hash, perfil) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Daniel Tabosa', 'daniel@manutec.local', 'ainda-sem-seguranca', 'admin'),
    ('22222222-2222-2222-2222-222222222222', 'Josenildo Barbosa', 'josenildo@manutec.local', 'ainda-sem-seguranca', 'tecnico');

INSERT INTO cliente (id, codigo_referencia, nome_condominio, endereco, bairro, cidade,
                     sindico_responsavel, telefone_contato, email_contato, tipo_atendimento) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', 'COND-001', 'Edifício Costa Azul',
     'Av. Boa Viagem, 3200', 'Boa Viagem', 'Recife',
     'Marcos Vinícius Andrade', '(81) 99812-4477', 'sindico@costaazul.local', 'Avulso');

INSERT INTO especificacao (id, marca, modelo, diametro_polegadas, tipo_registro, descricao_completa) VALUES
    ('bbbbbbbb-0000-0000-0000-000000000001', 'Bermad', '720', '2', 'Esfera', 'Bermad 720 - 2" Esfera'),
    ('bbbbbbbb-0000-0000-0000-000000000002', 'Cla-Val', '90-01', '1.1/2', 'Esfera', 'Cla-Val 90-01 - 1.1/2" Esfera');

INSERT INTO estacao (id, cliente_id, localizacao_instalacao, andar_inicial, andar_final, requer_fechamento_geral) VALUES
    ('cccccccc-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001',
     'Barrilete - Casa de bombas', 0, 4, TRUE),
    ('cccccccc-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001',
     'Shaft hidráulico - 5º pavimento', 5, 12, FALSE);

INSERT INTO valvula (id, estacao_id, especificacao_id, codigo_referencia, numero_valvula,
                     numero_serie, data_instalacao, periodicidade_meses) VALUES
    -- Estação 1: duas válvulas em paralelo
    ('dddddddd-0000-0000-0000-000000000001', 'cccccccc-0000-0000-0000-000000000001',
     'bbbbbbbb-0000-0000-0000-000000000001', 'COND-001-VRP-01', '01', 'BM-720-88412', '2019-06-12', 12),
    ('dddddddd-0000-0000-0000-000000000002', 'cccccccc-0000-0000-0000-000000000001',
     'bbbbbbbb-0000-0000-0000-000000000001', 'COND-001-VRP-02', '02', 'BM-720-88413', '2019-06-12', 12),
    -- Estação 2: uma válvula, ainda sem manutenção lançada
    ('dddddddd-0000-0000-0000-000000000003', 'cccccccc-0000-0000-0000-000000000002',
     'bbbbbbbb-0000-0000-0000-000000000002', 'COND-001-VRP-03', '01', 'CV-9001-20177', '2019-06-12', 12);

-- Datas relativas a hoje, para o painel exibir os três status de uma vez.
INSERT INTO servico (valvula_id, data_realizada, tipo_manutencao, tecnico_responsavel_id,
                     pecas_substituidas, pressao_entrada, pressao_saida, observacoes, criado_por_id) VALUES
    -- VENCIDO: 14 meses atrás
    ('dddddddd-0000-0000-0000-000000000001', CURRENT_DATE - INTERVAL '14 months', 'Preventiva',
     '22222222-2222-2222-2222-222222222222', 'Kit de reparo diafragma', 6.20, 3.50,
     'Ajuste de pressão de saída.', '11111111-1111-1111-1111-111111111111'),
    -- PRÓXIMO DO VENCIMENTO: 11 meses e meio atrás
    ('dddddddd-0000-0000-0000-000000000002', CURRENT_DATE - INTERVAL '11 months 20 days', 'Preventiva',
     '22222222-2222-2222-2222-222222222222', 'Mola do piloto', 6.10, 3.40,
     'Limpeza do filtro Y a montante.', '11111111-1111-1111-1111-111111111111');

-- A válvula COND-001-VRP-03 fica sem serviço de propósito: aparece como SEM REGISTRO.
