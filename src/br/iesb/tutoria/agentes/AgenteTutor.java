package br.iesb.tutoria.agentes;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.HashMap;
import java.util.Map;

public class AgenteTutor extends Agent {

    private final Map<String, String> planoPedagogico = new HashMap<>();
    private final Map<String, String> preRequisitos   = new HashMap<>();

    // limiar de dificuldade critica — acima disso solicita 2 recursos
    private static final double LIMIAR_CRITICO = 0.90;

    @Override
    protected void setup() {
        System.out.println("[Tutor] iniciado: " + getAID().getName());

        preRequisitos.put("topico:fracoes",      "topico:operacoes_basicas");
        preRequisitos.put("topico:equacoes",     "topico:fracoes");
        preRequisitos.put("topico:geometria",    "topico:operacoes_basicas");
        preRequisitos.put("topico:porcentagem",  "topico:fracoes");
        preRequisitos.put("topico:estatistica",  "topico:porcentagem");
        preRequisitos.put("topico:algebra",      "topico:equacoes");
        preRequisitos.put("topico:trigonometria","topico:geometria");

        planoPedagogico.put("topico:fracoes",      "NORMAL");
        planoPedagogico.put("topico:equacoes",     "NORMAL");
        planoPedagogico.put("topico:geometria",    "NORMAL");
        planoPedagogico.put("topico:porcentagem",  "NORMAL");
        planoPedagogico.put("topico:estatistica",  "NORMAL");
        planoPedagogico.put("topico:algebra",      "NORMAL");
        planoPedagogico.put("topico:trigonometria","NORMAL");

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    processarMensagem(msg.getContent());
                } else {
                    block();
                }
            }
        });
    }

    private void processarMensagem(String conteudo) {
        System.out.println("[Tutor] recebido: " + conteudo);
        if (conteudo.startsWith("LACUNA")) {
            tratarLacuna(conteudo);
        }
    }

    private void tratarLacuna(String conteudo) {
        String[] partes = conteudo.split("\\|");
        if (partes.length < 2) return;

        String topico = partes[1].trim();

        planoPedagogico.put(topico, "REVISAO");
        System.out.println("[Tutor] plano atualizado: " + topico + " -> REVISAO");

        String preReq = preRequisitos.get(topico);
        if (preReq != null) {
            System.out.println("[Tutor] pre-requisito: " + preReq);
        }

        // consulta dificuldade atual no painel para decidir quantos recursos solicitar
        Map<String, double[]> dados = br.iesb.tutoria.ui.PainelSMA.getInstancia()
                .getDadosTopicos();
        double dificuldadeAtual = 0.5; // valor padrao se ainda nao ha dados
        if (dados.containsKey(topico)) {
            dificuldadeAtual = dados.get(topico)[0];
        }

        int qtdRecursos = dificuldadeAtual >= LIMIAR_CRITICO ? 2 : 1;

        System.out.println("[Tutor] dificuldade atual de " + topico
                + ": " + String.format("%.2f", dificuldadeAtual)
                + " -> solicitando " + qtdRecursos + " recurso(s)");

        solicitarRecursos(topico, preReq, qtdRecursos);
        reportarAoCoordenador(topico, qtdRecursos);
    }

    private void solicitarRecursos(String topico, String preRequisito, int quantidade) {
        ACLMessage sol = new ACLMessage(ACLMessage.REQUEST);
        sol.addReceiver(new AID("Seletor", AID.ISLOCALNAME));
        sol.setConversationId("solicita-recurso");
        String conteudo = "SOLICITA|" + topico
                + "|nivel:basico|tipo:revisao"
                + "|quantidade:" + quantidade;
        if (preRequisito != null) conteudo += "|pre_req:" + preRequisito;
        sol.setContent(conteudo);
        send(sol);
        System.out.println("[Tutor] -> Seletor: solicita " + quantidade
                + " recurso(s) para " + topico);
        br.iesb.tutoria.ui.PainelSMA.getInstancia()
                .registrarMensagem("Tutor", "Seletor",
                        "solicita " + quantidade + " recurso(s) para " + topico);
    }

    private void reportarAoCoordenador(String topico, int qtdRecursos) {
        ACLMessage rel = new ACLMessage(ACLMessage.INFORM);
        rel.addReceiver(new AID("Coordenador", AID.ISLOCALNAME));
        rel.setConversationId("relatorio-tutor");
        rel.setContent("RELATORIO|acao:redirecionar_revisao|" + topico
                + "|origem:Tutor|recursos:" + qtdRecursos);
        send(rel);
        System.out.println("[Tutor] -> Coordenador: redirecionamento de " + topico);
        br.iesb.tutoria.ui.PainelSMA.getInstancia()
                .registrarMensagem("Tutor", "Coordenador",
                        "redirecionamento de " + topico
                                + " (" + qtdRecursos + " recurso(s))");
    }
}