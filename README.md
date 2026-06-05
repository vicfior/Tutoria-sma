# SMA Tutoria Inteligente Personalizada

Sistema Multiagente para tutoria inteligente personalizada, desenvolvido em Java com a plataforma JADE. O sistema detecta dificuldades de aprendizagem em tempo real e gera intervenções pedagógicas adaptadas ao perfil do aluno, sem depender de modelos de linguagem externos.

Projeto desenvolvido para a disciplina de **Inteligência Artificial II** — IESB, semestre 01/2026.

---

## Visão geral

O sistema é composto por **7 agentes autônomos** que cooperam via protocolo FIPA-ACL:

| Agente | Tipo arquitetural | Responsabilidade |
|---|---|---|
| `AgenteAluno` | Reativo c/ modelos | Simula o estudante; consome recursos pedagógicos |
| `AgenteMonitor` | Reativo c/ modelos | Detecta padrões de erro em tempo real |
| `AgenteDiagnostico` | Reativo c/ modelos | Acumula evidências e identifica lacunas conceituais |
| `AgenteTutor` | Baseado em objetivos | Planeja a intervenção pedagógica |
| `AgenteAvaliador` | Com aprendizagem | Calibra dificuldade percebida por tópico via recompensa/penalidade |
| `AgenteSeletor` | Baseado em utilidade | Seleciona recursos por função de utilidade |
| `AgenteCoordenador` | Baseado em utilidade | Arbitra e executa a intervenção final |

### Fluxo de cooperação

```
Aluno → Monitor → Diagnóstico → Tutor → Seletor → Coordenador → Aluno
```

Quando a dificuldade percebida D(t) ≥ 0,90, o Tutor solicita automaticamente **2 recursos** ao Seletor, produzindo redução de 40pp na probabilidade de erro em uma única intervenção.

---

## Pré-requisitos

- **Java 17** ou superior
- **IntelliJ IDEA** (Community ou Ultimate)
- **JADE 4.6.0** — baixe em [https://jade.tilab.com](https://jade.tilab.com) → Downloads → JADE 4.6.0 All-in-one

> O `jade.jar` não está incluído no repositório. Após o download, localize o arquivo em `JADE-all-4.6.0/jade/lib/jade.jar`.

---

## Configuração no IntelliJ IDEA

**1. Clone o repositório**

```bash
git clone https://github.com/seu-usuario/tutoria-sma-jade.git
cd tutoria-sma-jade
```

**2. Abra o projeto no IntelliJ**

File → Open → selecione a pasta `tutoria-sma-jade`.

**3. Configure o SDK**

File → Project Structure → Project → SDK → selecione Java 17+.

**4. Adicione o JADE como dependência**

File → Project Structure → Modules → Dependencies → `+` → JARs or Directories → navegue até `jade.jar` → OK → Apply.

**5. Marque a pasta `src` como Sources Root**

Clique com o botão direito na pasta `src` → Mark Directory as → Sources Root.

---

## Execução

**1. Configure o Run**

Run → Edit Configurations → `+` → Application:
- **Name:** `SMA Tutoria`
- **Main class:** `br.iesb.tutoria.Main`
- **Program arguments:** *(deixe vazio)*

**2. Execute**

Clique em **Run** ou pressione `Shift+F10`.

**O que esperar ao executar:**

- Uma janela Swing abre automaticamente — o **Painel de Monitoramento** em tempo real
- O console exibe o log completo de mensagens entre agentes
- O sistema começa a simular o aluno percorrendo 7 tópicos de matemática
- Após detectar lacunas, intervenções pedagógicas são disparadas automaticamente

**Para encerrar:**

Feche a janela do painel ou clique no botão Stop no IntelliJ. Ao encerrar, três arquivos são gerados automaticamente na raiz do projeto:

```
relatorio-sma-TIMESTAMP.txt        — relatório final da sessão
grafico1_taxa_acerto_TIMESTAMP.png  — taxa de acerto por tópico
grafico2_dificuldade_TIMESTAMP.png  — dificuldade percebida por tópico
grafico3_linha_TIMESTAMP.png        — taxa de acerto vs D(t)
```

---

## Estrutura do projeto

```
src/
└── br/iesb/tutoria/
    ├── Main.java                      — ponto de entrada; sobe JADE e o painel
    ├── agentes/
    │   ├── AgenteAluno.java
    │   ├── AgenteMonitor.java
    │   ├── AgenteDiagnostico.java
    │   ├── AgenteTutor.java
    │   ├── AgenteAvaliador.java
    │   ├── AgenteSeletor.java
    │   └── AgenteCoordenador.java
    └── ui/
        ├── PainelSMA.java             — painel Swing de monitoramento em tempo real
        └── GeradorGraficos.java       — geração de gráficos PNG com Java2D
```

---

## Tópicos simulados

O sistema simula um aluno em 7 tópicos de matemática com probabilidades de erro iniciais distintas:

| Tópico | Prob. erro inicial |
|---|---|
| Frações | 85% |
| Álgebra | 75% |
| Trigonometria | 80% |
| Estatística | 60% |
| Equações | 50% |
| Porcentagem | 30% |
| Geometria | 25% |

Cada intervenção reduz a probabilidade de erro em 20pp (piso: 15%). Quando D(t) ≥ 0,90, duas intervenções são aplicadas simultaneamente (−40pp).

---

## Tecnologias

- **Java** — linguagem de implementação
- **JADE 4.6.0** — middleware para Sistemas Multiagentes (FIPA-ACL)
- **Java Swing** — painel de monitoramento
- **Java2D** — geração de gráficos PNG (sem dependências externas além do JADE)

---

## Referências

- WOOLDRIDGE, M. *An Introduction to MultiAgent Systems*. 2. ed. Wiley, 2009.
- RUSSELL, S.; NORVIG, P. *Artificial Intelligence: A Modern Approach*. 4. ed. Pearson, 2022.
- BELLIFEMINE, F. L.; CAIRE, G.; GREENWOOD, D. *Developing Multi-Agent Systems with JADE*. Wiley, 2007.
- LIU, Y. et al. AgentTutor. arXiv:2601.04219, 2025.
- TOKOLI, B. et al. ALIGNAgent. arXiv:2601.15551, 2026.
- MARQUES, M. F. et al. Knowledge Transfer in Agent Organizations. ENIAC 2025. DOI: 10.5753/eniac.2025.14399.
- ROCHA, M. et al. Applying Theory of Mind to Multi-agent Systems. BRACIS 2023. DOI: 10.1007/978-3-031-45368-7_24.

---

## Autores

- Victória Thereza Lopes Fior — 2312130079
- Caio Silveira — 2312130166

IESB — Inteligência Artificial II — Prof. Letícia — 01/2026
