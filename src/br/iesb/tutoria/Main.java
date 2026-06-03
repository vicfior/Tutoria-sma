package br.iesb.tutoria;

import br.iesb.tutoria.ui.PainelSMA;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;

public class Main {
    public static void main(String[] args) throws Exception {
        // 1. inicia o painel grafico ANTES dos agentes
        PainelSMA painel = PainelSMA.getInstancia();

        // 2. instala o shutdown hook para imprimir relatorio ao fechar
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // relatorio textual
            String relatorio = painel.gerarRelatorioFinal();
            System.out.println(relatorio);

            try (java.io.PrintWriter pw = new java.io.PrintWriter(
                    "relatorio-sma-" + System.currentTimeMillis() + ".txt")) {
                pw.println(relatorio);
                System.out.println("Relatorio salvo em arquivo.");
            } catch (Exception ignored) {}

            // graficos PNG
            System.out.println("[Main] Gerando graficos PNG...");
            br.iesb.tutoria.ui.GeradorGraficos.gerarTodos(painel.getDadosTopicos());
        }));

        // 3. sobe a plataforma JADE
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.MAIN_HOST, "localhost");
        p.setParameter(Profile.GUI, "true");

        AgentContainer container = rt.createMainContainer(p);

        container.createNewAgent("Aluno",       "br.iesb.tutoria.agentes.AgenteAluno",       null).start();
        container.createNewAgent("Monitor",     "br.iesb.tutoria.agentes.AgenteMonitor",     null).start();
        container.createNewAgent("Diagnostico", "br.iesb.tutoria.agentes.AgenteDiagnostico", null).start();
        container.createNewAgent("Tutor",       "br.iesb.tutoria.agentes.AgenteTutor",       null).start();
        container.createNewAgent("Avaliador",   "br.iesb.tutoria.agentes.AgenteAvaliador",   null).start();
        container.createNewAgent("Seletor",     "br.iesb.tutoria.agentes.AgenteSeletor",     null).start();
        container.createNewAgent("Coordenador", "br.iesb.tutoria.agentes.AgenteCoordenador", null).start();
    }
}