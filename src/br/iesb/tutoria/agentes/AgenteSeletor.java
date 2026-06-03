package br.iesb.tutoria.agentes;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AgenteSeletor extends Agent {

    private static class Recurso {
        String id, topico, tipo;
        int nivel, duracaoMin;

        Recurso(String id, String topico, String tipo, int nivel, int dur) {
            this.id = id; this.topico = topico; this.tipo = tipo;
            this.nivel = nivel; this.duracaoMin = dur;
        }
    }

    private final List<Recurso> repositorio = new ArrayList<>();

    @Override
    protected void setup() {
        System.out.println("[Seletor] iniciado: " + getAID().getName());

        // === FRACOES ===
        repositorio.add(new Recurso("REC_001", "topico:fracoes", "video",      1,  8));
        repositorio.add(new Recurso("REC_002", "topico:fracoes", "exercicio",  1, 15));
        repositorio.add(new Recurso("REC_003", "topico:fracoes", "texto",      2, 20));
        repositorio.add(new Recurso("REC_004", "topico:fracoes", "flashcard",  1,  5));

        // === EQUACOES ===
        repositorio.add(new Recurso("REC_005", "topico:equacoes", "video",     1, 10));
        repositorio.add(new Recurso("REC_006", "topico:equacoes", "exercicio", 2, 15));
        repositorio.add(new Recurso("REC_007", "topico:equacoes", "flashcard", 1,  5));

        // === GEOMETRIA ===
        repositorio.add(new Recurso("REC_008", "topico:geometria", "video",    1, 12));
        repositorio.add(new Recurso("REC_009", "topico:geometria", "texto",    1, 18));
        repositorio.add(new Recurso("REC_010", "topico:geometria", "flashcard",1,  5));

        // === PORCENTAGEM ===
        repositorio.add(new Recurso("REC_011", "topico:porcentagem", "exercicio", 1, 10));
        repositorio.add(new Recurso("REC_012", "topico:porcentagem", "flashcard", 1,  5));
        repositorio.add(new Recurso("REC_013", "topico:porcentagem", "video",     1,  8));

        // === ESTATISTICA ===
        repositorio.add(new Recurso("REC_014", "topico:estatistica", "video",     1, 14));
        repositorio.add(new Recurso("REC_015", "topico:estatistica", "exercicio", 2, 20));
        repositorio.add(new Recurso("REC_016", "topico:estatistica", "flashcard", 1,  6));
        repositorio.add(new Recurso("REC_017", "topico:estatistica", "texto",     1, 15));

        // === ALGEBRA ===
        repositorio.add(new Recurso("REC_018", "topico:algebra", "video",      1, 12));
        repositorio.add(new Recurso("REC_019", "topico:algebra", "exercicio",  1, 20));
        repositorio.add(new Recurso("REC_020", "topico:algebra", "flashcard",  1,  5));
        repositorio.add(new Recurso("REC_021", "topico:algebra", "texto",      2, 25));

        // === TRIGONOMETRIA ===
        repositorio.add(new Recurso("REC_022", "topico:trigonometria", "video",     1, 15));
        repositorio.add(new Recurso("REC_023", "topico:trigonometria", "flashcard", 1,  6));
        repositorio.add(new Recurso("REC_024", "topico:trigonometria", "exercicio", 2, 20));
        repositorio.add(new Recurso("REC_025", "topico:trigonometria", "texto",     1, 18));

        System.out.println("[Seletor] repositorio carregado: "
                + repositorio.size() + " recursos");

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    processarSolicitacao(msg.getContent());
                } else {
                    block();
                }
            }
        });
    }

    private void processarSolicitacao(String conteudo) {
        System.out.println("[Seletor] recebido: " + conteudo);

        String[] partes = conteudo.split("\\|");
        if (partes.length < 2) return;

        String topicoAlvo = partes[1].trim();

        // extrai quantidade solicitada (padrao: 1)
        int quantidade = 1;
        for (String p : partes) {
            if (p.startsWith("quantidade:")) {
                try { quantidade = Integer.parseInt(p.substring("quantidade:".length()).trim()); }
                catch (NumberFormatException ignored) {}
            }
        }

        // seleciona os N melhores recursos distintos por utilidade
        List<Recurso> melhores = repositorio.stream()
                .filter(r -> r.topico.equals(topicoAlvo))
                .sorted((a, b) -> Double.compare(
                        calcularUtilidade(b), calcularUtilidade(a)))
                .limit(quantidade)
                .collect(java.util.stream.Collectors.toList());

        if (melhores.isEmpty()) {
            System.out.println("[Seletor] AVISO: nenhum recurso para [" + topicoAlvo + "]");
            return;
        }

        System.out.println("[Seletor] " + melhores.size() + " recurso(s) selecionado(s) para "
                + topicoAlvo + ":");
        for (Recurso r : melhores) {
            System.out.println("[Seletor]   " + r.id + " (" + r.tipo
                    + ", nivel " + r.nivel + ", " + r.duracaoMin + " min)");
            entregarRecurso(r, topicoAlvo);
        }
    }

    // funcao de utilidade: pondera tipo, nivel e duracao
    private double calcularUtilidade(Recurso r) {
        double u = 0;

        // tipo: flashcard e video sao preferidos para revisao rapida
        switch (r.tipo) {
            case "flashcard": u += 0.6; break;  // revisao rapida e eficaz
            case "video":     u += 0.5; break;
            case "exercicio": u += 0.3; break;
            case "texto":     u += 0.2; break;
        }

        // nivel basico tem prioridade em contexto de revisao
        if (r.nivel == 1)      u += 0.4;
        else if (r.nivel == 2) u += 0.2;

        // recursos mais curtos sao preferidos (engajamento)
        if (r.duracaoMin <= 6)  u += 0.3;
        else if (r.duracaoMin <= 10) u += 0.2;
        else if (r.duracaoMin <= 15) u += 0.1;

        return u;
    }

    private void entregarRecurso(Recurso r, String topico) {
        ACLMessage resp = new ACLMessage(ACLMessage.INFORM);
        resp.addReceiver(new AID("Tutor", AID.ISLOCALNAME));
        resp.setConversationId("entrega-recurso");
        resp.setContent("ENTREGA|recurso_id:" + r.id
                + "|tipo:" + r.tipo
                + "|nivel:" + r.nivel
                + "|" + topico);
        send(resp);
        System.out.println("[Seletor] -> Tutor: entrega " + r.id
                + " (" + r.tipo + ")");
        br.iesb.tutoria.ui.PainelSMA.getInstancia()
                .registrarMensagem("Seletor", "Tutor",
                        "entrega " + r.id + " (" + r.tipo + ")");

        ACLMessage rel = new ACLMessage(ACLMessage.INFORM);
        rel.addReceiver(new AID("Coordenador", AID.ISLOCALNAME));
        rel.setConversationId("relatorio-seletor");
        rel.setContent("RELATORIO|acao:recurso_selecionado|"
                + topico + "|origem:Seletor|recurso:" + r.id);
        send(rel);
        System.out.println("[Seletor] -> Coordenador: recurso " + r.id);
        br.iesb.tutoria.ui.PainelSMA.getInstancia()
                .registrarMensagem("Seletor", "Coordenador",
                        "recurso " + r.id + " (" + r.tipo + ")");
    }
}