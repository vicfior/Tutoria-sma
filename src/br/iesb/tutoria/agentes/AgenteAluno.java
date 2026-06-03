package br.iesb.tutoria.agentes;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AgenteAluno extends Agent {

    private final Random random = new Random();

    // identificadores internos (sem acento) — usados nas mensagens entre agentes
    private String[] topicos = {
            "topico:fracoes",
            "topico:equacoes",
            "topico:geometria",
            "topico:porcentagem",
            "topico:estatistica",
            "topico:algebra",
            "topico:trigonometria"
    };

    // labels para exibicao no console
    private String[] labels = {
            "Fracoes", "Equacoes", "Geometria", "Porcentagem",
            "Estatistica", "Algebra", "Trigonometria"
    };

    // probabilidade inicial de erro por topico (0-100)
    // aluno tem muita dificuldade em fracoes, algebra e trigonometria
    private int[] probErroInicial = { 85, 50, 25, 30, 60, 75, 80 };

    private Map<String, Integer> probErroAtual      = new HashMap<>();
    private Map<String, Integer> recursosConsumidos = new HashMap<>();

    private int topicoIndex  = 0;
    private int questaoAtual = 1;

    @Override
    protected void setup() {
        System.out.println("[Aluno] iniciado: " + getAID().getName());
        System.out.println("[Aluno] iniciando sessao de estudos simulada...");

        for (int i = 0; i < topicos.length; i++) {
            probErroAtual.put(topicos[i], probErroInicial[i]);
            recursosConsumidos.put(topicos[i], 0);
        }

        enviarEvento("ACESSO_QUESTAO|" + topicos[topicoIndex]);

        addBehaviour(new TickerBehaviour(this, 5_000) {
            @Override
            protected void onTick() {
                simularInteracao();
            }
        });

        // filtra APENAS mensagens de intervencao pedagogica do Coordenador
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                jade.lang.acl.MessageTemplate filtro =
                        jade.lang.acl.MessageTemplate.MatchConversationId("intervencao-pedagogica");
                ACLMessage msg = receive(filtro);
                if (msg != null) {
                    processarIntervencao(msg.getContent());
                } else {
                    block();
                }
            }
        });
    }

    private void simularInteracao() {
        String topico = topicos[topicoIndex];
        String label  = labels[topicoIndex];
        int prob      = probErroAtual.get(topico);

        boolean errou = random.nextInt(100) < prob;

        if (errou) {
            System.out.println("[Aluno] errou questao " + questaoAtual
                    + " em " + label + " (prob.erro: " + prob + "%)");
            enviarEvento("ERRO|" + topico + "|questao:" + questaoAtual);
        } else {
            System.out.println("[Aluno] acertou questao " + questaoAtual
                    + " em " + label + " (prob.erro: " + prob + "%)");
            enviarEvento("ACERTO|" + topico + "|questao:" + questaoAtual);
        }

        questaoAtual++;
        if (questaoAtual > 5) {
            questaoAtual = 1;
            topicoIndex  = (topicoIndex + 1) % topicos.length;
            System.out.println("[Aluno] avancando para: " + labels[topicoIndex]);
            enviarEvento("ACESSO_QUESTAO|" + topicos[topicoIndex]);
        }
    }

    private void processarIntervencao(String conteudo) {
        if (!conteudo.startsWith("INTERVENCAO")) return;

        System.out.println("[Aluno] INTERVENCAO recebida: [" + conteudo + "]");
        System.out.println("[Aluno] mapa de probabilidades atual: " + probErroAtual);

        String topicoAlvo = null;
        String recurso    = "REC";
        for (String p : conteudo.split("\\|")) {
            String parte = p.trim();
            if (parte.startsWith("topico:")) topicoAlvo = parte;
            else if (parte.startsWith("recurso:")) recurso = parte.substring("recurso:".length()).trim();
        }

        if (topicoAlvo == null) {
            System.out.println("[Aluno] ERRO: topico nao encontrado na mensagem!");
            return;
        }

        // busca a chave exata no mapa
        Integer probAtualObj = probErroAtual.get(topicoAlvo);
        if (probAtualObj == null) {
            for (String chave : probErroAtual.keySet()) {
                if (chave.trim().equals(topicoAlvo.trim())) {
                    topicoAlvo   = chave;
                    probAtualObj = probErroAtual.get(chave);
                    break;
                }
            }
        }
        if (probAtualObj == null) {
            System.out.println("[Aluno] ERRO: topico [" + topicoAlvo + "] nao existe no mapa!");
            return;
        }

        int probAtual = probAtualObj;
        // cada recurso reduz 20pp, com piso de 15%
        int novaProb  = Math.max(15, probAtual - 20);
        probErroAtual.put(topicoAlvo, novaProb);
        recursosConsumidos.merge(topicoAlvo, 1, Integer::sum);

        System.out.println("[Aluno] *** APRENDIZAGEM ***");
        System.out.println("[Aluno] consumiu " + recurso + " sobre " + topicoAlvo);
        System.out.println("[Aluno] prob.erro: " + probAtual + "% -> " + novaProb + "%"
                + (novaProb < probAtual - 20 + 1 ? " (piso atingido)" : ""));
        System.out.println("[Aluno] total de recursos em " + topicoAlvo
                + ": " + recursosConsumidos.get(topicoAlvo));
        System.out.println("[Aluno] mapa atualizado: " + probErroAtual);
        System.out.println("[Aluno] *********************");
    }

    private void enviarEvento(String conteudo) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("Monitor", AID.ISLOCALNAME));
        msg.setConversationId("sessao-aluno");
        msg.setContent(conteudo);
        send(msg);
        System.out.println("[Aluno] -> Monitor: " + conteudo);
        br.iesb.tutoria.ui.PainelSMA.getInstancia()
                .registrarMensagem("Aluno", "Monitor", conteudo);

        if (conteudo.startsWith("ACERTO") || conteudo.startsWith("ERRO")) {
            ACLMessage copia = new ACLMessage(ACLMessage.INFORM);
            copia.addReceiver(new AID("Avaliador", AID.ISLOCALNAME));
            copia.setConversationId("resposta-aluno");
            copia.setContent(conteudo);
            send(copia);
        }
    }
}