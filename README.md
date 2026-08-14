# Valtis

Sistema de controle de manutenção de válvulas redutoras de pressão (VRP) da **Manutec Válvulas**.

Registra as manutenções executadas em campo e agenda automaticamente o retorno de 12 meses no Google Calendar, transformando manutenções passadas em oportunidades de reventa rastreáveis.

---

## O problema

A Manutec instala e mantém VRPs em prédios. Toda manutenção abre uma janela de reventa cerca de 12 meses depois. Hoje esse rastreamento é manual — alguém garimpa registros antigos em momentos vagos. O resultado é mensurável:

| Situação da base real (2018 – ago/2026) | Número |
|---|---|
| NFs de manutenção emitidas | 595 |
| Clientes distintos atendidos | 296 |
| Clientes há **24+ meses** sem manutenção (crítico) | **153** |
| Clientes há **12+ meses** sem manutenção (atrasado) | 52 |
| Clientes recorrentes **sem nenhum contato em 2026** | **94** |

**69% dos clientes já atendidos estão fora do ciclo de 12 meses.** Essa é a receita que o sistema existe para recuperar.

## O que o sistema faz

1. O técnico lança a manutenção executada pelo celular ou pelo PC.
2. O sistema calcula o vencimento do próximo ciclo (padrão: 12 meses).
3. Cria dois eventos na agenda da empresa — 30 dias antes (contato comercial) e 7 dias antes (lembrete final) — convidando os 3 usuários.
4. Mantém um painel de status de todas as válvulas: vencidas, próximas do vencimento, em dia e sem registro.
5. Gera o relatório de serviço em PDF para envio ao síndico.

## Estado atual

> **Fase de documentação. Não há código implementado.**

Os requisitos, a arquitetura e o modelo de dados estão definidos. A implementação começa pela Fase 1 descrita no roadmap abaixo.

## Documentação

Leia nesta ordem:

| Documento | Para quê |
|---|---|
| **[AGENTS.md](AGENTS.md)** | **Obrigatório antes de qualquer alteração no repositório.** Regras de arquitetura, código, testes, Git, segurança e o que exige aprovação humana |
| [docs/requirements.md](docs/requirements.md) | O **que** construir. Requisitos funcionais (RF), não funcionais (RNF) e regras de negócio (RN), com IDs estáveis |
| [docs/architecture.md](docs/architecture.md) | **Como** construir. Camadas, fronteiras, padrão Outbox, módulos e stack |
| [docs/database.md](docs/database.md) | O modelo de dados, convenções de nomenclatura, mapeamento da planilha legada e o glossário do domínio |
| [docs/decisions.md](docs/decisions.md) | **Por que** está assim. Registro de decisões (ADR), append-only |
| [_arquivo/](_arquivo/) | Versões anteriores da documentação. Histórico, **não** fonte de verdade |

**Regra de ouro:** cada fato mora em um único arquivo. Os demais referenciam por ID (`RF-26`, `RN-04`, `D-08`). Se você encontrar a mesma informação em dois lugares, é bug de documentação — reporte.

## Stack

TypeScript · Next.js (App Router) · PostgreSQL · Prisma · Supabase (banco, storage e auth) · Tailwind + shadcn/ui · Google Calendar API

Detalhes e alternativas descartadas em [docs/architecture.md](docs/architecture.md).

## Como rodar

Ainda não aplicável — não há código. Esta seção será preenchida na Fase 1.

## Roadmap

| Fase | Escopo | Marco |
|---|---|---|
| **0 — Fundação** | Conta Google, agenda compartilhada, app OAuth publicado, repositório e deploy vazio | Um evento criado pelo sistema aparece na agenda dos 3 celulares |
| **1 — MVP + carga da base** | Acesso, cadastros, lançamento, painel, recorrência, Calendar, importação dos 296 clientes | No primeiro dia de uso o painel já mostra 153 clientes críticos como lista de trabalho |
| **2 — Fotos e relatório** | Anexos e PDF do serviço | O técnico envia o relatório ao síndico no fim do atendimento |
| **3 — Pipeline comercial** | Fila de oportunidades, desfecho, indicadores | Responder "quantos clientes recuperamos neste trimestre?" com um número |
| **4 — Refinamentos** | Exportações, QR Code, notificações | Sob demanda |

## Usuários

Três pessoas da Manutec, usando celular em campo e PC no escritório. Perfis: **técnico** e **administrador**.

## Contexto do negócio

**VRP** = Válvula Redutora de Pressão. **Estação redutora** = conjunto que contém várias VRPs, atendendo uma faixa de andares. A estação é a unidade que a empresa fatura, e por isso é a unidade central do modelo de dados. Glossário completo em [docs/database.md](docs/database.md#glossário-do-domínio).
