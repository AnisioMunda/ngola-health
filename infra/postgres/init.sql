-- ============================================================
-- HospitalAO — PostgreSQL init.sql
-- ============================================================
-- Este script corre automaticamente quando o container
-- PostgreSQL arranca pela PRIMEIRA vez.
--
-- Nas vezes seguintes (quando o volume já existe com dados),
-- este script NÃO corre de novo — os dados são preservados.
--
-- As tabelas do sistema (pacientes, consultas, etc.) são
-- criadas mais tarde pelo Liquibase (a partir do Sprint 1).
-- Este script apenas prepara o ambiente base.
-- ============================================================


-- ------------------------------------------------------------
-- EXTENSÕES
-- ------------------------------------------------------------

-- uuid-ossp: permite gerar UUIDs com a função uuid_generate_v4()
-- Todas as tabelas do sistema usam UUID como chave primária.
-- Exemplo de uso nas tabelas:
--   id UUID PRIMARY KEY DEFAULT uuid_generate_v4()
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- pgcrypto: funções de criptografia para dados sensíveis
-- Permite encriptar/desencriptar dados directamente no SQL.
-- Usado para: NIF de pacientes, dados clínicos sensíveis.
-- Exemplo de uso:
--   pgp_sym_encrypt('dado sensível', 'chave_secreta')
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- unaccent: remove acentos em pesquisas de texto
-- Permite que uma pesquisa por "jose" encontre "José".
-- Muito útil na pesquisa de nomes de pacientes angolanos
-- que têm acentos e caracteres especiais.
CREATE EXTENSION IF NOT EXISTS "unaccent";


-- ------------------------------------------------------------
-- CONFIGURAÇÕES DE TIMEZONE E LOCALE
-- ------------------------------------------------------------

-- Timezone de Angola: WAT (West Africa Time) = UTC+1
-- CRÍTICO para sistema hospitalar: garante que todas as datas
-- e horas guardadas na BD estão correctas para Luanda.
-- Sem isto, os timestamps ficam desfasados 1 hora.
ALTER DATABASE hospitalao SET timezone TO 'Africa/Luanda';

-- Confirmar a configuração (aparece nos logs do container)
DO $$
BEGIN
  RAISE NOTICE 'Timezone configurado: Africa/Luanda (WAT = UTC+1)';
END
$$;


-- ------------------------------------------------------------
-- SCHEMAS
-- ------------------------------------------------------------

-- Schema público (já existe por defeito no PostgreSQL)
-- Todas as tabelas do sistema ficam aqui:
-- pacientes, episodios, consultas, faturas, etc.
-- (não precisamos criar, mas deixamos o comentário para clareza)
-- CREATE SCHEMA IF NOT EXISTS public;

-- Schema de auditoria: regista todas as alterações importantes
-- Quem alterou o quê, quando, e qual era o valor anterior.
-- Obrigatório em sistemas de saúde por razões legais.
-- Exemplo: se um diagnóstico for alterado, fica registado quem
-- fez a alteração e quando.
CREATE SCHEMA IF NOT EXISTS auditoria;

COMMENT ON SCHEMA auditoria IS
  'Registo de auditoria: histórico de alterações a dados clínicos e financeiros';

COMMENT ON SCHEMA public IS
  'Schema principal: tabelas do sistema HospitalAO';


-- ------------------------------------------------------------
-- TIPOS ENUMERADOS GLOBAIS
-- ------------------------------------------------------------
-- Definir tipos enum aqui em vez de nas tabelas individuais
-- permite reutilizá-los em várias tabelas e garante consistência.

-- Sexo biológico do paciente (usado em triagem e relatórios)
DO $$ BEGIN
  CREATE TYPE sexo_enum AS ENUM ('MASCULINO', 'FEMININO');
EXCEPTION
  WHEN duplicate_object THEN NULL; -- Ignorar se já existir
END $$;

-- Estado geral de registos (activo/inactivo em vez de apagar)
-- Nunca apagamos dados num sistema hospitalar — apenas desactivamos
DO $$ BEGIN
  CREATE TYPE estado_registo_enum AS ENUM ('ACTIVO', 'INACTIVO');
EXCEPTION
  WHEN duplicate_object THEN NULL;
END $$;


-- ------------------------------------------------------------
-- FUNÇÃO DE AUDITORIA
-- ------------------------------------------------------------
-- Esta função vai ser usada pelos triggers de auditoria
-- que serão criados nas migrations do Sprint 1 em diante.
-- Regista automaticamente quem alterou o quê e quando.

CREATE OR REPLACE FUNCTION auditoria.registar_alteracao()
RETURNS TRIGGER AS $$
BEGIN
  -- Inserir na tabela de auditoria (criada no Sprint 1)
  -- Por agora a função existe mas a tabela ainda não
  -- (será criada pelo Liquibase no Sprint 1)
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION auditoria.registar_alteracao() IS
  'Trigger function: regista alterações para auditoria clínica e financeira';


-- ------------------------------------------------------------
-- MENSAGEM FINAL DE CONFIRMAÇÃO
-- ------------------------------------------------------------
DO $$
BEGIN
  RAISE NOTICE '============================================';
  RAISE NOTICE 'HospitalAO — Base de dados inicializada';
  RAISE NOTICE 'Extensões: uuid-ossp, pgcrypto, unaccent';
  RAISE NOTICE 'Timezone: Africa/Luanda (WAT)';
  RAISE NOTICE 'Schemas: public, auditoria';
  RAISE NOTICE 'As tabelas serão criadas pelo Liquibase.';
  RAISE NOTICE '============================================';
END
$$;