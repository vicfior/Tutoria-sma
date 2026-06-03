package br.iesb.tutoria.agentes;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.HashMap;
import java.util.Map;

public class AgenteDiagnostico extends Agent {

    private final Map<String, Integer> lacunas = new HashMap<>();

    @Override
    protected void setup() {
        System.out.println("[Diagnostico] iniciado: " + getAID().getName());

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    processarAlerta(msg.getContent());
                } else {
                    block();
                }
            }
        });
    }

    private void processarAlerta(String conteudo) {
        System.out.println("[Diagnostico] recebido: " + conteudo);

        String[] partes = conteudo.split("\\|");
        if (partes.length < 3) return;

        String tipoAlerta = partes[1].trim();
        String topico     = partes[2].trim();

        lacunas.merge(topico, 1, Integer::sum);
        int ocorrencias = lacunas.get(topico);

        System.out.println("[Diagnostico] lacuna -> " + topico
                + " (" + ocorrencias + " ocorrencia(s))");

        if (ocorrencias >= 2) {
            reportarLacuna(topico, tipoAlerta, ocorrencias);
            lacunas.put(topico, 0);
        }
    }

    private void reportarLacuna(String topico, String causa, int ocorrencias) {
        ACLMessage relatorio = new ACLMessage(ACLMessage.INFORM);
        // nome exato registrado no Main.java
        relatorio.addReceiver(new AID("Tutor", AID.ISLOCALNAME));
        relatorio.setConversationId("lacuna-diagnostico");
        relatorio.setContent("LACUNA|" + topico
                + "|causa:" + causa
                + "|ocorrencias:" + ocorrencias);
        send(relatorio);
        System.out.println("[Diagnostico] -> Tutor: LACUNA em " + topico);
        br.iesb.tutoria.ui.PainelSMA.getInstancia()
                .registrarMensagem("Diagnostico", "Tutor", "LACUNA em " + topico);
    }
}