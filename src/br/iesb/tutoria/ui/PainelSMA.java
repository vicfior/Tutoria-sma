package br.iesb.tutoria.ui;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PainelSMA {

    private static PainelSMA instancia;

    private JFrame janela;
    private JTextPane logArea;
    private JPanel painelTopicos;
    private JLabel lblResumo;
    private JLabel lblIntervencoes;
    private JLabel lblMensagens;

    private final Map<String, EstatisticaTopico> stats = new ConcurrentHashMap<>();
    private final Map<String, JPanel>       cardsTopicos = new ConcurrentHashMap<>();
    private final Map<String, JProgressBar> barrasDif    = new ConcurrentHashMap<>();
    private final Map<String, JLabel>       labelsStats  = new ConcurrentHashMap<>();

    private int totalMensagens    = 0;
    private int totalIntervencoes = 0;
    private final List<String> historicoIntervencoes = new ArrayList<>();
    private final long inicio = System.currentTimeMillis();

    private final Map<String, Color> coresAgentes = new HashMap<>();

    // ============================================================
    // DICIONARIO DE FORMATACAO — identificador interno -> exibicao
    // ============================================================
    private static final Map<String, String> DISPLAY = new HashMap<>();
    static {
        // nomes de agentes
        DISPLAY.put("Aluno",        "Aluno");
        DISPLAY.put("Monitor",      "Monitor");
        DISPLAY.put("Diagnostico",  "Diagnóstico");
        DISPLAY.put("Tutor",        "Tutor");
        DISPLAY.put("Avaliador",    "Avaliador");
        DISPLAY.put("Seletor",      "Seletor");
        DISPLAY.put("Coordenador",  "Coordenador");
        DISPLAY.put("INTERVENCAO",  "Intervenção");

        // topicos
        DISPLAY.put("topico:fracoes",          "Frações");
        DISPLAY.put("topico:equacoes",         "Equações");
        DISPLAY.put("topico:geometria",        "Geometria");
        DISPLAY.put("topico:porcentagem",      "Porcentagem");
        DISPLAY.put("topico:estatistica",      "Estatística");
        DISPLAY.put("topico:algebra",          "Álgebra");
        DISPLAY.put("topico:trigonometria",    "Trigonometria");
        DISPLAY.put("topico:operacoes_basicas","Operações Básicas");

        // tipos de mensagem
        DISPLAY.put("ACESSO_QUESTAO",      "Acessou questão");
        DISPLAY.put("ERRO",                "Errou");
        DISPLAY.put("ACERTO",              "Acertou");
        DISPLAY.put("ALERTA",              "Alerta");
        DISPLAY.put("ERROS_CONSECUTIVOS",  "Erros consecutivos");
        DISPLAY.put("TEMPO_EXCEDIDO",      "Tempo excedido");
        DISPLAY.put("LACUNA",              "Lacuna detectada");
        DISPLAY.put("SOLICITA",            "Solicitando recurso");
        DISPLAY.put("ENTREGA",             "Recurso entregue");
        DISPLAY.put("RELATORIO",           "Relatório");
        DISPLAY.put("INTERVENCAO",         "Intervenção pedagógica");

        // tipos de recurso
        DISPLAY.put("video",     "Vídeo");
        DISPLAY.put("exercicio", "Exercício");
        DISPLAY.put("texto",     "Texto");
        DISPLAY.put("flashcard", "Flashcard");
    }

    /** Converte um identificador interno para texto legível em português. */
    private static String fmt(String raw) {
        if (raw == null) return "";
        raw = raw.trim();
        // tenta lookup direto
        if (DISPLAY.containsKey(raw)) return DISPLAY.get(raw);
        // tenta substituir partes conhecidas
        String resultado = raw;
        for (Map.Entry<String, String> e : DISPLAY.entrySet()) {
            resultado = resultado.replace(e.getKey(), e.getValue());
        }
        // remove underscores e pipe sobrando
        resultado = resultado.replace("_", " ").replace("|", " › ");
        return resultado;
    }

    /** Formata uma mensagem completa de forma legível. */
    private static String fmtMensagem(String conteudo) {
        if (conteudo == null) return "";
        // extrai o primeiro campo (tipo) e substitui o resto
        String[] partes = conteudo.split("\\|");
        if (partes.length == 1) return fmt(conteudo);

        StringBuilder sb = new StringBuilder();
        sb.append(fmt(partes[0].trim()));
        for (int i = 1; i < partes.length; i++) {
            String p = partes[i].trim();
            if (!p.isEmpty()) sb.append(" › ").append(fmt(p));
        }
        return sb.toString();
    }

    private PainelSMA() {
        coresAgentes.put("Aluno",       new Color(160, 160, 160));
        coresAgentes.put("Monitor",     new Color( 29, 158, 117));
        coresAgentes.put("Diagnostico", new Color(216,  90,  48));
        coresAgentes.put("Diagnóstico", new Color(216,  90,  48));
        coresAgentes.put("Tutor",       new Color(127, 119, 221));
        coresAgentes.put("Avaliador",   new Color(186, 117,  23));
        coresAgentes.put("Seletor",     new Color( 24,  95, 165));
        coresAgentes.put("Coordenador", new Color( 59, 109,  17));
        coresAgentes.put("Intervenção", new Color(220,  60,  60));
        coresAgentes.put("INTERVENCAO", new Color(220,  60,  60));
    }

    public static synchronized PainelSMA getInstancia() {
        if (instancia == null) {
            instancia = new PainelSMA();
            SwingUtilities.invokeLater(() -> instancia.criarJanela());
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        return instancia;
    }

    private void criarJanela() {
        janela = new JFrame("SMA — Tutoria Inteligente: Painel de Monitoramento");
        janela.setSize(1500, 900);
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        janela.setLocationRelativeTo(null);

        JPanel topo = new JPanel(new GridLayout(1, 3, 14, 0));
        topo.setBorder(new EmptyBorder(14, 14, 8, 14));
        lblMensagens    = criarCard("Mensagens trocadas",        "0",   new Color(100,100,100));
        lblIntervencoes = criarCard("Intervenções pedagógicas",  "0",   new Color( 59,109, 17));
        lblResumo       = criarCard("Tempo decorrido",           "0s",  new Color( 29,158,117));
        topo.add(lblMensagens.getParent());
        topo.add(lblIntervencoes.getParent());
        topo.add(lblResumo.getParent());

        painelTopicos = new JPanel();
        painelTopicos.setLayout(new BoxLayout(painelTopicos, BoxLayout.Y_AXIS));
        TitledBorder bt = new TitledBorder("Tópicos — Dificuldade Percebida (Avaliador)");
        bt.setTitleFont(new Font("Sans", Font.BOLD, 15));
        painelTopicos.setBorder(bt);
        JScrollPane scrollTopicos = new JScrollPane(painelTopicos);
        scrollTopicos.setPreferredSize(new Dimension(520, 0));

        logArea = new JTextPane();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 18));
        logArea.setBackground(new Color(28, 28, 33));
        JScrollPane scrollLog = new JScrollPane(logArea);
        TitledBorder bl = new TitledBorder("Fluxo de mensagens entre agentes");
        bl.setTitleFont(new Font("Sans", Font.BOLD, 15));
        scrollLog.setBorder(bl);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollTopicos, scrollLog);
        split.setDividerLocation(540);

        janela.setLayout(new BorderLayout());
        janela.add(topo,  BorderLayout.NORTH);
        janela.add(split, BorderLayout.CENTER);
        janela.setVisible(true);

        new Timer(1000, e -> atualizarTempo()).start();
    }

    private JLabel criarCard(String titulo, String valor, Color cor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cor, 3, true),
                new EmptyBorder(14, 18, 14, 18)));
        card.setBackground(Color.WHITE);

        JLabel tit = new JLabel(titulo);
        tit.setFont(new Font("Sans", Font.PLAIN, 15));
        tit.setForeground(new Color(80, 80, 80));

        JLabel val = new JLabel(valor);
        val.setFont(new Font("Sans", Font.BOLD, 36));
        val.setForeground(cor);

        card.add(tit, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        return val;
    }

    // ====================================================
    // API PUBLICA
    // ====================================================

    public void registrarMensagem(String origem, String destino, String conteudo) {
        totalMensagens++;
        String origemFmt  = fmt(origem);
        String destinoFmt = fmt(destino);
        String conteudoFmt = fmtMensagem(conteudo);
        SwingUtilities.invokeLater(() -> {
            appendLog(origem, origemFmt, destino, destinoFmt, conteudoFmt);
            lblMensagens.setText(String.valueOf(totalMensagens));
        });
    }

    public void registrarEvento(String agente, String evento) {
        String agenFmt = fmt(agente);
        String evtFmt  = fmtMensagem(evento);
        SwingUtilities.invokeLater(() -> appendLog(agente, agenFmt, null, null, evtFmt));
    }

    public void atualizarTopico(String topico, double dificuldade, int acertos, int erros) {
        stats.computeIfAbsent(topico, EstatisticaTopico::new)
                .atualizar(dificuldade, acertos, erros);
        SwingUtilities.invokeLater(() -> renderizarTopico(topico));
    }

    public void registrarIntervencao(String topico, String recurso) {
        totalIntervencoes++;
        String topicoFmt = fmt(topico);
        String linha = formatarHora() + " — " + topicoFmt + " ← " + recurso;
        historicoIntervencoes.add(linha);
        SwingUtilities.invokeLater(() -> {
            lblIntervencoes.setText(String.valueOf(totalIntervencoes));
            appendLog("INTERVENCAO", "Intervenção", null, null,
                    "★ " + topicoFmt + " recebeu recurso " + recurso + " ★");
        });
    }

    // ====================================================
    // RENDERIZACAO
    // ====================================================

    private void renderizarTopico(String topico) {
        EstatisticaTopico s = stats.get(topico);
        if (s == null) return;

        String topicoLabel = fmt(topico);

        JPanel card = cardsTopicos.get(topico);
        if (card == null) {
            card = new JPanel(new BorderLayout(6, 8));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                    new EmptyBorder(12, 14, 12, 14)));
            card.setMaximumSize(new Dimension(520, 130));
            card.setBackground(Color.WHITE);

            JLabel titulo = new JLabel(topicoLabel.toUpperCase());
            titulo.setFont(new Font("Sans", Font.BOLD, 17));

            JProgressBar barra = new JProgressBar(0, 100);
            barra.setStringPainted(true);
            barra.setPreferredSize(new Dimension(0, 34));
            barra.setFont(new Font("Sans", Font.BOLD, 14));
            barrasDif.put(topico, barra);

            JLabel info = new JLabel(" ");
            info.setFont(new Font("Sans", Font.PLAIN, 14));
            info.setForeground(new Color(80, 80, 80));
            labelsStats.put(topico, info);

            card.add(titulo, BorderLayout.NORTH);
            card.add(barra,  BorderLayout.CENTER);
            card.add(info,   BorderLayout.SOUTH);

            cardsTopicos.put(topico, card);
            painelTopicos.add(card);
            painelTopicos.add(Box.createVerticalStrut(10));
            painelTopicos.revalidate();
        }

        JProgressBar barra = barrasDif.get(topico);
        int valor = (int) Math.round(s.dificuldade * 100);
        barra.setValue(valor);
        barra.setString(valor + "% dificuldade");

        if (valor >= 70)      barra.setForeground(new Color(216, 90, 48));
        else if (valor >= 40) barra.setForeground(new Color(186, 117, 23));
        else                  barra.setForeground(new Color(29, 158, 117));

        int total = s.acertos + s.erros;
        double taxa = total == 0 ? 0 : 100.0 * s.acertos / total;
        labelsStats.get(topico).setText(String.format(
                "Acertos: %d   Erros: %d   Taxa de acerto: %.0f%%",
                s.acertos, s.erros, taxa));
    }

    private void appendLog(String origemKey, String origemLabel,
                           String destinoKey, String destinoLabel,
                           String conteudo) {
        try {
            javax.swing.text.StyledDocument doc = logArea.getStyledDocument();

            // horario
            javax.swing.text.SimpleAttributeSet atTime = new javax.swing.text.SimpleAttributeSet();
            javax.swing.text.StyleConstants.setForeground(atTime, new Color(120, 120, 120));

            // origem
            javax.swing.text.SimpleAttributeSet atOrig = new javax.swing.text.SimpleAttributeSet();
            Color corOrig = coresAgentes.getOrDefault(origemKey,
                    coresAgentes.getOrDefault(origemLabel, new Color(200,200,200)));
            javax.swing.text.StyleConstants.setForeground(atOrig, corOrig);
            javax.swing.text.StyleConstants.setBold(atOrig, true);

            // mensagem
            javax.swing.text.SimpleAttributeSet atMsg = new javax.swing.text.SimpleAttributeSet();
            javax.swing.text.StyleConstants.setForeground(atMsg, new Color(210, 210, 210));

            doc.insertString(doc.getLength(), formatarHora() + "  ", atTime);
            doc.insertString(doc.getLength(), "[" + origemLabel + "] ", atOrig);

            if (destinoKey != null && destinoLabel != null) {
                javax.swing.text.SimpleAttributeSet atDest = new javax.swing.text.SimpleAttributeSet();
                Color corDest = coresAgentes.getOrDefault(destinoKey,
                        coresAgentes.getOrDefault(destinoLabel, new Color(200,200,200)));
                javax.swing.text.StyleConstants.setForeground(atDest, corDest);
                javax.swing.text.StyleConstants.setBold(atDest, true);
                doc.insertString(doc.getLength(), "→ [" + destinoLabel + "] ", atDest);
            }

            doc.insertString(doc.getLength(), conteudo + "\n", atMsg);
            logArea.setCaretPosition(doc.getLength());
        } catch (Exception ignored) {}
    }

    private void atualizarTempo() {
        long s = (System.currentTimeMillis() - inicio) / 1000;
        lblResumo.setText(String.format("%dm %ds", s / 60, s % 60));
    }

    private String formatarHora() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    // ====================================================
    // EXPORTACAO DE DADOS PARA O GERADOR DE GRAFICOS
    // ====================================================

    /**
     * Retorna os dados consolidados de cada topico para o GeradorGraficos.
     * Formato: topico -> double[]{dificuldade, acertos, erros}
     */
    public Map<String, double[]> getDadosTopicos() {
        Map<String, double[]> resultado = new LinkedHashMap<>();
        for (Map.Entry<String, EstatisticaTopico> e : stats.entrySet()) {
            EstatisticaTopico s = e.getValue();
            resultado.put(e.getKey(), new double[]{s.dificuldade, s.acertos, s.erros});
        }
        return resultado;
    }

    // ====================================================
    // RELATORIO FINAL
    // ====================================================
    public String gerarRelatorioFinal() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n============================================================\n");
        sb.append("     RELATÓRIO FINAL — SMA TUTORIA INTELIGENTE\n");
        sb.append("============================================================\n");

        long s = (System.currentTimeMillis() - inicio) / 1000;
        sb.append(String.format("Tempo total de sessão:      %dm %ds%n", s / 60, s % 60));
        sb.append(String.format("Mensagens trocadas:         %d%n",  totalMensagens));
        sb.append(String.format("Intervenções pedagógicas:   %d%n",  totalIntervencoes));
        sb.append("\n");

        sb.append("------ DESEMPENHO POR TÓPICO ------\n");
        for (Map.Entry<String, EstatisticaTopico> e : stats.entrySet()) {
            EstatisticaTopico s2 = e.getValue();
            int total = s2.acertos + s2.erros;
            double taxa = total == 0 ? 0 : 100.0 * s2.acertos / total;
            sb.append(String.format("%-22s | Acertos: %3d | Erros: %3d | Taxa: %5.1f%% | Dificuldade: %.2f%n",
                    fmt(e.getKey()), s2.acertos, s2.erros, taxa, s2.dificuldade));
        }
        sb.append("\n");

        sb.append("------ HISTÓRICO DE INTERVENÇÕES ------\n");
        if (historicoIntervencoes.isEmpty()) {
            sb.append("(nenhuma intervenção registrada)\n");
        } else {
            for (String linha : historicoIntervencoes) sb.append(linha).append("\n");
        }
        sb.append("\n");

        sb.append("------ AVALIAÇÃO PEDAGÓGICA ------\n");
        if (totalIntervencoes > 0) {
            sb.append("O SMA detectou e interveio em ")
                    .append(totalIntervencoes).append(" momento(s) de dificuldade.\n");
            sb.append("Tópico de maior dificuldade: ")
                    .append(fmt(maiorDificuldade())).append("\n");
            sb.append("Tópico de melhor desempenho: ")
                    .append(fmt(menorDificuldade())).append("\n");
        }
        sb.append("============================================================\n");
        return sb.toString();
    }

    private String maiorDificuldade() {
        return stats.entrySet().stream()
                .max(Comparator.comparingDouble(e -> e.getValue().dificuldade))
                .map(Map.Entry::getKey).orElse("(nenhum)");
    }

    private String menorDificuldade() {
        return stats.entrySet().stream()
                .min(Comparator.comparingDouble(e -> e.getValue().dificuldade))
                .map(Map.Entry::getKey).orElse("(nenhum)");
    }

    private static class EstatisticaTopico {
        double dificuldade;
        int acertos, erros;
        EstatisticaTopico(String t) {}
        void atualizar(double d, int a, int e) { dificuldade = d; acertos = a; erros = e; }
    }
}