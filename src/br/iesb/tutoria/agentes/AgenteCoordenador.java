package br.iesb.tutoria.agentes;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AgenteCoordenador extends Agent {

    private final Map<String, String> estadoGlobal = new HashMap<>();
    private final Map<String, Set<String>> agentesPorTopico = new HashMap<>();
    private final Map<String, String> recursoPorTopico = new HashMap<>();

    @Override
    protected void setup() {
        System.out.println("[Coordenador] iniciado: " + getAID().getName());

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    processarRelatorio(msg.getContent(), msg.getSender().getLocalName());
                } else {
                    block();
                }
            }
        });
    }

    private void processarRelatorio(String conteudo, String origem) {
        System.out.println("[Coordenador] recebido de " + origem + ": " + conteudo);

        String[] partes = conteudo.split("\\|");
        if (partes.length < 3) return;

        String acao   = partes[1];
        String topico = partes[2];

        // extrai recurso se vier do Seletor
        for (String p : partes) {
            if (p.startsWith("recurso:")) {
                recursoPorTopico.put(topico, p.substring("recurso:".length()));
            }
        }

        estadoGlobal.put(topico, acao);
        agentesPorTopico.computeIfAbsent(topico, k -> new HashSet<>()).add(origem);

        Set<String> agentes = agentesPorTopico.get(topico);
        System.out.println("[Coordenador] estado: " + topico + " -> " + acao
                + " (agentes: " + agentes + ")");

        // ARBITRAGEM PEDAGOGICA: Tutor + Seletor convergem
        if (agentes.contains("Tutor") && agentes.contains("Seletor")) {
            arbitragemPedagogica(topico);
            agentes.remove("Tutor");
            agentes.remove("Seletor");
        }
        else if (origem.equals("Avaliador")) {
            arbitragemCalibracao(topico, conteudo);
        }
    }

    private void arbitragemPedagogica(String topico) {
        String acao    = estadoGlobal.get(topico);
        String recurso = recursoPorTopico.getOrDefault(topico, "REC_default");

        System.out.println("[Coordenador] ========================================");
        System.out.println("[Coordenador] ARBITRAGEM PEDAGÓGICA CONCLUIDA");
        System.out.println("[Coordenador] tópico: " + topico);
        System.out.println("[Coordenador] ação final: " + acao);
        System.out.println("[Coordenador] recurso entregue: " + recurso);
        System.out.println("[Coordenador] cadeia: Tutor + Seletor convergiram");
        System.out.println("[Coordenador] executando intervenção no aluno...");
        System.out.println("[Coordenador] ========================================");

        // registra intervencao no painel grafico
        br.iesb.tutoria.ui.PainelSMA.getInstancia()
                .registrarIntervencao(topico, recurso);

        // envia intervencao ao Aluno
        ACLMessage intervencao = new ACLMessage(ACLMessage.INFORM);
        intervencao.addReceiver(new AID("Aluno", AID.ISLOCALNAME));
        intervencao.setConversationId("intervencao-pedagogica");
        intervencao.setContent("INTERVENCAO | " + topico + " | recurso: " + recurso);
        send(intervencao);
        System.out.println("[Coordenador] ➞ Aluno: intervenção entregue para "
                + topico);
        br.iesb.tutoria.ui.PainelSMA.getInstancia()
                .registrarMensagem("Coordenador", "Aluno",
                        "INTERVENÇÃO em " + topico);
    }

    private void arbitragemCalibracao(String topico, String conteudo) {
        String dificuldade = "";
        for (String p : conteudo.split("\\|")) {
            if (p.startsWith("dificuldade:")) {
                dificuldade = p.substring("dificuldade:".length());
            }
        }
        System.out.println("[Coordenador] >>> calibração do Avaliador registrada: "
                + topico + " (dificuldade aprendida: " + dificuldade + ")");
    }
}