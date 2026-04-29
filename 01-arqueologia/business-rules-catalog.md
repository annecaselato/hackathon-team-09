---
title: "Business Rules Catalog"
description: "Extracted business rules from SIFAP legacy Natural programs and Adabas DDMs"
author: "Paula Silva, AI-Native Software Engineer, Americas Global Black Belt at Microsoft"
date: "2026-04-29"
version: "1.2.0"
status: "approved"
tags: ["stage-1", "business-rules", "legacy", "analysis"]
---

# 📋 Business Rules Catalog — SIFAP Legacy

> Complete catalog of business rules extracted during Stage 1 archaeology, traced to actual source code from legacy Natural programs and Adabas DDMs.

---

---

## 👤 Beneficiary Rules (BR-BEN-*)

### BR-BEN-001: CPF deve ser válido por dígito verificador

**Fonte**: [legacy/natural-programs/VALBENEF.NSN](legacy/natural-programs/VALBENEF.NSN#L113), [VALBENEF.NSN](legacy/natural-programs/VALBENEF.NSN#L218), [VALBENEF.NSN](legacy/natural-programs/VALBENEF.NSN#L236)

**Regra**: CPF inválido reprova a validação cadastral.

**Impacto**: Resultado final da validação fica inválido e rejeita operação.

**Caso de teste**: CPF com dígito verificador incorreto deve falhar.

---

### BR-BEN-002: Nome deve conter nome e sobrenome

**Fonte**: [legacy/natural-programs/VALBENEF.NSN](legacy/natural-programs/VALBENEF.NSN#L264), [VALBENEF.NSN](legacy/natural-programs/VALBENEF.NSN#L270)

**Regra**: Nome em branco ou sem espaço de separação é inválido.

**Impacto**: Impede persistência de cadastro válido.

**Caso de teste**: Nome único sem sobrenome deve ser rejeitado.

---

### BR-BEN-003: Data de nascimento deve ser consistente

**Fonte**: [legacy/natural-programs/VALBENEF.NSN](legacy/natural-programs/VALBENEF.NSN#L248), [VALBENEF.NSN](legacy/natural-programs/VALBENEF.NSN#L252), [VALBENEF.NSN](legacy/natural-programs/VALBENEF.NSN#L256)

**Regra**: Ano entre 1900 e ano atual; mês entre 1 e 12; dia válido para o mês.

**Impacto**: Evita cadastro com data biologicamente impossível.

---

### BR-BEN-004: UF precisa estar na tabela válida

**Fonte**: [legacy/natural-programs/VALBENEF.NSN](legacy/natural-programs/VALBENEF.NSN#L145), [VALBENEF.NSN](legacy/natural-programs/VALBENEF.NSN#L148)

**Regra**: UF fora da lista de 27 UFs é inválida.

**Impacto**: Registro rejeitado se UF não está na tabela interna.

---

### BR-BEN-005: Status permitido no validador

**Fonte**: [legacy/natural-programs/VALBENEF.NSN](legacy/natural-programs/VALBENEF.NSN#L164)

**Regra**: Apenas A, S, C, I, D são aceitos.

**Impacto**: Status fora do domínio é inválido.

---

## 💳 Payment Rules (BR-PAY-*)

### BR-PAY-001: Não gerar duplicidade por competência

**Fonte**: [legacy/natural-programs/BATCHPGT.NSN](legacy/natural-programs/BATCHPGT.NSN#L202), [BATCHPGT.NSN](legacy/natural-programs/BATCHPGT.NSN#L335)

**Regra**: Se já existe pagamento da competência para o CPF, ignora nova geração.

**Impacto**: Evita pagamento duplicado no mesmo mês.

---

### BR-PAY-002: Domínio de status de pagamento

**Fonte**: [legacy/adabas-ddms/PAGAMENTO.ddm](legacy/adabas-ddms/PAGAMENTO.ddm#L48)

**Regra**: Status aceitos são P, G, E, C, D, X, R.

**Impacto**: Integrações e relatórios devem respeitar este domínio.

---

### BR-PAY-003: Teto de descontos com exceção judicial

**Fonte**: [legacy/natural-programs/CALCDSCT.NSN](legacy/natural-programs/CALCDSCT.NSN#L101), [CALCDSCT.NSN](legacy/natural-programs/CALCDSCT.NSN#L131), [CALCDSCT.NSN](legacy/natural-programs/CALCDSCT.NSN#L165)

**Regra**: Descontos não judiciais respeitam teto de 30%; judicial não tem teto.

**Impacto**: Valor líquido pode cair acima do teto quando houver ordem judicial.

---

### BR-PAY-004: Correção retroativa atualiza registro existente

**Fonte**: [legacy/natural-programs/CALCCORR.NSN](legacy/natural-programs/CALCCORR.NSN#L128), [CALCCORR.NSN](legacy/natural-programs/CALCCORR.NSN#L162)

**Regra**: Pagamento pode ser atualizado por rotina de correção.

**Impacto**: Trilha histórica de alterações deve considerar auditoria.

---

## 🧮 Calculation Rules (BR-CALC-*)

### BR-CALC-001: Cálculo mensal regular usa fatores completos

**Fonte**: [legacy/natural-programs/CALCBENF.NSN](legacy/natural-programs/CALCBENF.NSN#L225), [CALCBENF.NSN](legacy/natural-programs/CALCBENF.NSN#L229)

**Regra**: Base multiplicada por fatores de região, família, renda e idade, com reajuste.

**Impacto**: Valor varia por perfil socioeconômico.

---

### BR-CALC-002: 13º em dezembro

**Fonte**: [legacy/natural-programs/CALCBENF.NSN](legacy/natural-programs/CALCBENF.NSN#L243), [CALCBENF.NSN](legacy/natural-programs/CALCBENF.NSN#L245), [CALCBENF.NSN](legacy/natural-programs/CALCBENF.NSN#L249)

**Regra**: Em dezembro soma valor regular com valor do 13º.

**Impacto**: Bruto de dezembro é superior ao mês normal.

---

### BR-CALC-003: Abono para programa tipo A

**Fonte**: [legacy/natural-programs/CALCBENF.NSN](legacy/natural-programs/CALCBENF.NSN#L252), [CALCBENF.NSN](legacy/natural-programs/CALCBENF.NSN#L253), [CALCBENF.NSN](legacy/natural-programs/CALCBENF.NSN#L257)

**Regra**: Em dezembro, programa tipo A recebe abono de 15% do valor de benefício.

**Impacto**: Aumento adicional do bruto para subset de programas.

---

## 🔒 Audit Rules (BR-AUD-*)

### BR-AUD-001: Retenção longa de auditoria

**Fonte**: [legacy/adabas-ddms/AUDITORIA.ddm](legacy/adabas-ddms/AUDITORIA.ddm#L13), [AUDITORIA.ddm](legacy/adabas-ddms/AUDITORIA.ddm#L91)

**Regra**: Retenção mínima de 10 anos e histórico ativo sem purge.

**Impacto**: Migração deve preservar trilha extensa.

---

### BR-AUD-002: Relatório de auditoria filtra exclusões

**Fonte**: [legacy/natural-programs/RELAUDIT.NSN](legacy/natural-programs/RELAUDIT.NSN#L108)

**Regra**: Ação EX não aparece na listagem padrão.

**Impacto**: Risco de visão incompleta se usar apenas relatório.

---

## 💾 Data Retention Rules (BR-DATA-*)

### BR-DATA-001: Pagamento sem política de purge

**Fonte**: [legacy/adabas-ddms/PAGAMENTO.ddm](legacy/adabas-ddms/PAGAMENTO.ddm#L105), [PAGAMENTO.ddm](legacy/adabas-ddms/PAGAMENTO.ddm#L106)

**Regra**: Pagamentos históricos permanecem no próprio arquivo.

**Impacto**: Migração deve considerar carga histórica completa.

---

## 📊 Summary

**Total de Regras Documentadas**: 14

**Status**: Alinhado com fontes reais do legado deste repositório.

---

| Anterior | Home | Próximo |
|:---------|:----:|--------:|
| [← Glossário](glossary.md) | [Kit Home](../README.md) | [Mapa de Dependências →](dependency-map.md) |
