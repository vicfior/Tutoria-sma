package br.iesb.tutoria.agentes;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteMonitor extends Agent {

    private long   tempoInicioQuestao = System.currentTimeMillis();
    private int    errosConsecutivos  = 0;
    private String topicoAtual        = "desconhecido";

    private static final long LIMITE_TEMPO_MS = 60_000;
    private static final int  LIMITE_ERROS    = 3;

    @Override
    protected void setup() {
        System.out.println("[Monitor] iniciado: " + getAID().getName());

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    processarEvento(msg.getContent());
                } else {
                    block();
                }
            }
        });

        addBehaviour(new TickerBehaviour(this, 15_000) {
            @Override
            protected void onTick() {
                long gasto = System.currentTimeMillis() - tempoInicioQuestao;
                if (gasto > LIMITE_TEMPO_MS) {
                    enviarAlerta("TEMPO_EXCEDIDO", topicoAtual, gasto);
                    tempoInicioQuestao = System.currentTimeMillis();
                }
            }
        });
    }

    private void processarEvento(String evento) {
        System.out.println("[Monitor] evento: " + evento);

        // aceita tanto "ACESSO_QUESTAO" quanto "ACESSO QUESTAO" (com espaco)
        if (evento.startsWith("ACESSO")) {
            topicoAtual        = campo(evento, 1).trim();
            tempoInicioQuestao = System.currentTimeMillis();
            errosConsecutivos  = 0;

        } else if (evento.startsWith("ERRO")) {
            errosConsecutivos++;
            System.out.println("[Monitor] erros consecutivos: " + errosConsecutivos);
            if (errosConsecutivos >= LIMITE_ERROS) {
                enviarAlerta("ERROS_CONSECUTIVOS:" + errosConsecutivos,
                        campo(evento, 1).trim(), 0);
                errosConsecutivos = 0;
            }

        } else if (evento.startsWith("ACERTO")) {
            errosConsecutivos = 0;
        }
    }

    private void enviarAlerta(String tipo, String topico, long tempo) {
        ACLMessage alerta = new ACLMessage(ACLMessage.INFORM);
        // nome exato registrado no Main.java — sem acento
        alerta.addReceiver(new AID("Diagnostico", AID.ISLOCALNAME));
        alerta.setConversationId("alerta-sessao");
        alerta.setContent("ALERTA|" + tipo + "|" + topico + "|tempo:" + tempo);
        send(alerta);
        System.out.println("[Monitor] -> Diagnostico: ALERTA|" + tipo);
        br.iesb.tutoria.ui.PainelSMA.getInstancia()
                .registrarMensagem("Monitor", "Diagnostico", "ALERTA|" + tipo);
    }

    private String campo(String msg, int idx) {
        String[] p = msg.split("\\|");
        return idx < p.length ? p[idx] : "";
    }
}