package br.iesb.tutoria.agentes;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.HashMap;
import java.util.Map;

public class AgenteAvaliador extends Agent {

    // ========================================================
    // ARQUITETURA INTERNA — Russell & Norvig (2022), Aula_SMA2_c
    // ========================================================
    // 1. ELEMENTO DE DESEMPENHO: dificuldade percebida por topico
    private final Map<String, Double> dificuldadePercebida = new HashMap<>();

    // 2. CRITICO: avalia se o desempenho atual atende ao padrao esperado
    //    padrao de desempenho: taxa de acerto ideal = 0.7 (70%)
    private static final double TAXA_ACERTO_IDEAL = 0.7;

    // 3. ELEMENTO DE APRENDIZAGEM: contadores que alimentam o aprendizado
    private final Map<String, Integer> acertosPorTopico = new HashMap<>();
    private final Map<String, Integer> errosPorTopico   = new HashMap<>();

    // taxa de aprendizagem (quanto a dificuldade muda por feedback)
    private static final double TAXA_APRENDIZAGEM = 0.05;

    @Override
    protected void setup() {
        System.out.println("[Avaliador] iniciado: " + getAID().getName());
        System.out.println("[Avaliador] padrão de desempenho: "
                + (TAXA_ACERTO_IDEAL * 100) + "% de acerto");

        // recebe eventos do Aluno (mesmas mensagens enviadas ao Monitor)
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    processarResposta(msg.getContent());
                } else {
                    block();
                }
            }
        });

        // 4. GERADOR DE PROBLEMAS: a cada 60s, reporta calibracao ao Coordenador
        addBehaviour(new TickerBehaviour(this, 60_000) {
            @Override
            protected void onTick() {
                reportarCalibracao();
            }
        });
    }

    private void processarResposta(String conteudo) {
        // formato esperado: "ERRO|topico:fracoes|questao:X" ou "ACERTO|..."
        String[] partes = conteudo.split("\\|");
        if (partes.length < 2) return;

        String resultado = partes[0];
        String topico    = partes[1];

        // ELEMENTO DE APRENDIZAGEM: atualiza contadores
        if (resultado.equals("ACERTO")) {
            acertosPorTopico.merge(topico, 1, Integer::sum);
            ajustarDificuldade(topico, true);
        } else if (resultado.equals("ERRO")) {
            errosPorTopico.merge(topico, 1, Integer::sum);
            ajustarDificuldade(topico, false);
        } else {
            return; // ignora ACESSO_QUESTAO
        }

        // CRITICO: avalia se o desempenho atual atende ao padrao
        avaliarDesempenho(topico);
    }

    // ELEMENTO DE APRENDIZAGEM com mecanismo recompensa x penalidade
    private void ajustarDificuldade(String topico, boolean acertou) {
        double atual = dificuldadePercebida.getOrDefault(topico, 0.5);
        double novo;

        if (acertou) {
            // RECOMPENSA: aluno acertou — reduz dificuldade percebida
            novo = atual - TAXA_APRENDIZAGEM;
        } else {
            // PENALIDADE: aluno errou — aumenta dificuldade percebida
            novo = atual + TAXA_APRENDIZAGEM;
        }

        // limita valor entre 0.0 e 1.0
        novo = Math.max(0.0, Math.min(1.0, novo));
        dificuldadePercebida.put(topico, novo);
    }

    // CRITICO: compara desempenho atual com padrao esperado
    private void avaliarDesempenho(String topico) {
        int acertos = acertosPorTopico.getOrDefault(topico, 0);
        int erros   = errosPorTopico.getOrDefault(topico, 0);
        int total   = acertos + erros;

        // so avalia apos amostra minima de 5 questoes
        if (total < 5) return;

        double taxaAcerto = (double) acertos / total;
        double diferenca  = taxaAcerto - TAXA_ACERTO_IDEAL;

        // se o desempenho desviou significativamente do padrao, reporta
        if (Math.abs(diferenca) > 0.2 && total % 5 == 0) {
            String status = diferenca < 0 ? "ABAIXO DO IDEAL" : "ACIMA DO IDEAL";
            System.out.println("[Avaliador] crítico: " + topico
                    + " esta " + status
                    + " (taxa: " + String.format("%.2f", taxaAcerto)
                    + ", dificuldade percebida: "
                    + String.format("%.2f", dificuldadePercebida.get(topico)) + ")");
        }
    }

    // GERADOR DE PROBLEMAS: reporta calibracao consolidada ao Coordenador
    private void reportarCalibracao() {
        if (dificuldadePercebida.isEmpty()) return;

        System.out.println("[Avaliador] ----- calibração aprendida -----");
        dificuldadePercebida.forEach((topico, dif) -> {
            int ac = acertosPorTopico.getOrDefault(topico, 0);
            int er = errosPorTopico.getOrDefault(topico, 0);
            System.out.println("[Avaliador]   " + topico
                    + ": dificuldade = " + String.format("%.2f", dif)
                    + " (acertos = " + ac + ", erros = " + er + ")");

            // atualiza painel grafico
            br.iesb.tutoria.ui.PainelSMA.getInstancia()
                    .atualizarTopico(topico, dif, ac, er);
        });
        System.out.println("[Avaliador] --------------------------------");

        // envia ao Coordenador o topico de maior dificuldade aprendida
        String topicoMaisDificil = dificuldadePercebida.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (topicoMaisDificil != null) {
            ACLMessage rel = new ACLMessage(ACLMessage.INFORM);
            rel.addReceiver(new AID("Coordenador", AID.ISLOCALNAME));
            rel.setConversationId("relatório avaliador");
            rel.setContent("RELATÓRIO | ação: calibração aprendida"
                    + " | " + topicoMaisDificil
                    + " | origem: Avaliador"
                    + " | dificuldade:"
                    + String.format("%.2f", dificuldadePercebida.get(topicoMaisDificil)));
            send(rel);
            System.out.println("[Avaliador] ➞ Coordenador: tópico mais difícil = "
                    + topicoMaisDificil);
        }
    }
}