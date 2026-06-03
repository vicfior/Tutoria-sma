package br.iesb.tutoria.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Gera graficos PNG com os resultados da sessao do SMA.
 * Nao tem dependencias externas — usa apenas Java2D.
 */
public class GeradorGraficos {

    private static final Color COR_VERDE    = new Color(29, 158, 117);
    private static final Color COR_AMARELO  = new Color(186, 117, 23);
    private static final Color COR_VERMELHO = new Color(216, 90, 48);
    private static final Color COR_GRADE    = new Color(230, 230, 230);
    private static final Color COR_TEXTO    = new Color(50, 50, 50);
    private static final Color COR_SUBTIT   = new Color(120, 120, 120);
    private static final Color COR_LINHA1   = new Color(29, 158, 117);
    private static final Color COR_LINHA2   = new Color(216, 90, 48);

    private static final int W = 900;
    private static final int H = 500;
    private static final int ML = 80, MR = 40, MT = 75, MB = 90;

    // dicionario de labels com acentuacao correta
    private static final Map<String, String> LABELS = new LinkedHashMap<>();
    static {
        LABELS.put("topico:fracoes",      "Fra\u00e7\u00f5es");
        LABELS.put("topico:equacoes",     "Equa\u00e7\u00f5es");
        LABELS.put("topico:geometria",    "Geometria");
        LABELS.put("topico:porcentagem",  "Porcentagem");
        LABELS.put("topico:estatistica",  "Estat\u00edstica");
        LABELS.put("topico:algebra",      "\u00c1lgebra");
        LABELS.put("topico:trigonometria","Trigonometria");
    }

    // ── API publica ─────────────────────────────────────────────

    public static void gerarTodos(Map<String, double[]> dadosTopicos) {
        if (dadosTopicos == null || dadosTopicos.isEmpty()) {
            System.out.println("[GeradorGraficos] Nenhum dado disponivel.");
            return;
        }
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        try {
            gerarGraficoBarras(dadosTopicos, "grafico1_taxa_acerto_" + ts + ".png");
            System.out.println("[GeradorGraficos] Grafico 1 (barras taxa) salvo.");
        } catch (Exception e) {
            System.out.println("[GeradorGraficos] Erro grafico 1: " + e.getMessage());
        }
        try {
            gerarGraficoCombinado(dadosTopicos, "grafico2_combinado_" + ts + ".png");
            System.out.println("[GeradorGraficos] Grafico 2 (combinado lado a lado) salvo.");
        } catch (Exception e) {
            System.out.println("[GeradorGraficos] Erro grafico 2: " + e.getMessage());
        }
        try {
            gerarGraficoLinha(dadosTopicos, "grafico3_linha_" + ts + ".png");
            System.out.println("[GeradorGraficos] Grafico 3 (linha) salvo.");
        } catch (Exception e) {
            System.out.println("[GeradorGraficos] Erro grafico 3: " + e.getMessage());
        }
    }

    // ── GRAFICO COMBINADO: dificuldade + linha lado a lado ───────

    private static void gerarGraficoCombinado(Map<String, double[]> dados, String arquivo)
            throws Exception {

        // gera os dois sub-graficos em imagens separadas
        BufferedImage imgDif  = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        BufferedImage imgLinha = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

        renderizarDificuldade(dados, imgDif.createGraphics(),  0, 0, W, H);
        renderizarLinha(dados,       imgLinha.createGraphics(), 0, 0, W, H);

        // concatena lado a lado com separador de 2px
        int sepW    = 2;
        int totalW  = W * 2 + sepW;
        BufferedImage combinado = new BufferedImage(totalW, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D gc = combinado.createGraphics();
        gc.setColor(Color.WHITE);
        gc.fillRect(0, 0, totalW, H);

        gc.drawImage(imgDif,   0,       0, null);

        // linha separadora
        gc.setColor(new Color(210, 210, 210));
        gc.fillRect(W, 0, sepW, H);

        gc.drawImage(imgLinha, W + sepW, 0, null);
        gc.dispose();

        salvar(combinado, arquivo);
    }

    // ── GRAFICO 1: barras horizontais — taxa de acerto ──────────

    private static void gerarGraficoBarras(Map<String, double[]> dados, String arquivo)
            throws Exception {

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = configurar(img);

        desenharTitulo(g,
                "Taxa de acerto final por t\u00f3pico",
                "Propor\u00e7\u00e3o de acertos acumulados durante a sess\u00e3o (%)");

        List<String> topicos = new ArrayList<>(dados.keySet());
        int n = topicos.size();
        if (n == 0) return;

        int areaW = W - ML - MR - 160;
        int areaH = H - MT - MB;

        // grade vertical
        for (int v = 0; v <= 100; v += 20) {
            int x = ML + (int)(v / 100.0 * areaW);
            g.setColor(COR_GRADE);
            g.drawLine(x, MT, x, MT + areaH);
            g.setColor(COR_SUBTIT);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            String lbl = v + "%";
            int lw = g.getFontMetrics().stringWidth(lbl);
            g.drawString(lbl, x - lw / 2, MT + areaH + 18);
        }
        g.setColor(new Color(200, 200, 200));
        g.drawLine(ML, MT + areaH, ML + areaW, MT + areaH);

        int altBarra   = Math.min(38, (areaH / n) - 10);
        int espacamento = areaH / n;

        for (int i = 0; i < n; i++) {
            String topico = topicos.get(i);
            double[] d    = dados.get(topico);
            int acertos   = (int) d[1];
            int erros     = (int) d[2];
            int total     = acertos + erros;
            double taxa   = total == 0 ? 0 : 100.0 * acertos / total;

            int y      = MT + i * espacamento + (espacamento - altBarra) / 2;
            int largura = (int)(taxa / 100.0 * areaW);

            g.setColor(corPorTaxa(taxa));
            g.fillRoundRect(ML, y, Math.max(4, largura), altBarra, 6, 6);

            String label = LABELS.getOrDefault(topico,
                    topico.replace("topico:", ""));
            g.setColor(COR_TEXTO);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            int lw = g.getFontMetrics().stringWidth(label);
            g.drawString(label, ML - lw - 10, y + altBarra / 2 + 4);

            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            String val = String.format("%.0f%%", taxa);
            int vw = g.getFontMetrics().stringWidth(val);
            if (largura > vw + 10) {
                g.setColor(Color.WHITE);
                g.drawString(val, ML + largura - vw - 6, y + altBarra / 2 + 4);
            } else {
                g.setColor(COR_TEXTO);
                g.drawString(val, ML + largura + 4, y + altBarra / 2 + 4);
            }

            g.setColor(COR_SUBTIT);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.drawString("(" + acertos + " acertos / " + erros + " erros)",
                    ML + areaW + 8, y + altBarra / 2 + 4);
        }

        int ly = H - 28;
        desenharPilula(g, ML,      ly, COR_VERDE,
                "\u2265 70% \u2014 bom desempenho");
        desenharPilula(g, ML + 200, ly, COR_AMARELO,
                "40\u201369% \u2014 intermedi\u00e1rio");
        desenharPilula(g, ML + 400, ly, COR_VERMELHO,
                "< 40% \u2014 dificuldade");

        salvar(img, arquivo);
    }

    // ── GRAFICO 2: barras verticais — dificuldade percebida ─────

    private static void gerarGraficoDificuldade(Map<String, double[]> dados, String arquivo)
            throws Exception {

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = configurar(img);

        desenharTitulo(g,
                "Dificuldade percebida por t\u00f3pico (Agente Avaliador)",
                "Estimativa ao final da sess\u00e3o: 0,0 = dom\u00ednio total, 1,0 = m\u00e1xima dificuldade");

        List<String> topicos = new ArrayList<>(dados.keySet());
        int n = topicos.size();
        if (n == 0) return;

        int areaW = W - ML - MR - 80;
        int areaH = H - MT - MB;

        // grade horizontal
        for (int v = 0; v <= 10; v += 2) {
            double val = v / 10.0;
            int y = MT + areaH - (int)(val * areaH);
            g.setColor(COR_GRADE);
            g.drawLine(ML, y, ML + areaW, y);
            g.setColor(COR_SUBTIT);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.drawString(String.format("%.1f", val).replace(".", ","), ML - 32, y + 4);
        }

        // linha limiar 0.7
        int yRef = MT + areaH - (int)(0.7 * areaH);
        g.setColor(new Color(216, 90, 48, 130));
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10, new float[]{6, 4}, 0));
        g.drawLine(ML, yRef, ML + areaW, yRef);
        g.setStroke(new BasicStroke(1));
        g.setColor(COR_VERMELHO);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.drawString("limiar 0,7", ML + areaW + 4, yRef + 4);

        g.setColor(new Color(200, 200, 200));
        g.drawLine(ML, MT, ML, MT + areaH);
        g.drawLine(ML, MT + areaH, ML + areaW, MT + areaH);

        int largBarra = Math.max(20, areaW / n - 14);
        int passo     = areaW / n;

        for (int i = 0; i < n; i++) {
            String topico = topicos.get(i);
            double dif    = dados.get(topico)[0];

            int x      = ML + i * passo + (passo - largBarra) / 2;
            int altBar = (int)(dif * areaH);
            int y      = MT + areaH - altBar;

            g.setColor(corPorDificuldade(dif));
            g.fillRoundRect(x, y, largBarra, Math.max(4, altBar), 6, 6);

            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            String val = String.format("%.2f", dif).replace(".", ",");
            int vw = g.getFontMetrics().stringWidth(val);
            g.setColor(COR_TEXTO);
            g.drawString(val, x + largBarra / 2 - vw / 2, y - 5);

            String label = LABELS.getOrDefault(topico,
                    topico.replace("topico:", ""));
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            Graphics2D gr = (Graphics2D) g.create();
            gr.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gr.translate(x + largBarra / 2, MT + areaH + 14);
            gr.rotate(Math.toRadians(35));
            gr.setColor(COR_TEXTO);
            gr.drawString(label, 0, 0);
            gr.dispose();
        }

        int ly = H - 28;
        desenharPilula(g, ML,      ly, COR_VERDE,
                "< 0,4 \u2014 dom\u00ednio bom");
        desenharPilula(g, ML + 180, ly, COR_AMARELO,
                "0,4\u20130,7 \u2014 aten\u00e7\u00e3o");
        desenharPilula(g, ML + 360, ly, COR_VERMELHO,
                "> 0,7 \u2014 dificuldade alta");

        salvar(img, arquivo);
    }

    // ── GRAFICO 3: linha — taxa de acerto vs dificuldade ────────

    private static void gerarGraficoLinha(Map<String, double[]> dados, String arquivo)
            throws Exception {

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = configurar(img);

        desenharTitulo(g,
                "Taxa de acerto e dificuldade percebida por t\u00f3pico",
                "Compara\u00e7\u00e3o entre desempenho observado e estimativa do Agente Avaliador");

        List<String> topicos = new ArrayList<>(dados.keySet());
        int n = topicos.size();
        if (n == 0) return;

        int areaW = W - ML - MR - 20;
        int areaH = H - MT - MB;

        // grade horizontal
        for (int v = 0; v <= 10; v += 2) {
            double val = v / 10.0;
            int y = MT + areaH - (int)(val * areaH);
            g.setColor(COR_GRADE);
            g.drawLine(ML, y, ML + areaW, y);
            g.setColor(COR_SUBTIT);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.drawString(String.format("%.0f%%", val * 100), ML - 32, y + 4);
        }

        g.setColor(new Color(200, 200, 200));
        g.drawLine(ML, MT, ML, MT + areaH);
        g.drawLine(ML, MT + areaH, ML + areaW, MT + areaH);

        int passo = areaW / (n - 1 == 0 ? 1 : n - 1);

        // pontos das duas series
        int[] xPts  = new int[n];
        int[] yTaxa = new int[n];
        int[] yDif  = new int[n];

        for (int i = 0; i < n; i++) {
            String topico = topicos.get(i);
            double[] d    = dados.get(topico);
            int acertos   = (int) d[1];
            int erros     = (int) d[2];
            int total     = acertos + erros;
            double taxa   = total == 0 ? 0 : acertos / (double) total;
            double dif    = d[0];

            xPts[i]  = ML + i * passo;
            yTaxa[i] = MT + areaH - (int)(taxa * areaH);
            yDif[i]  = MT + areaH - (int)(dif * areaH);
        }

        // linha taxa de acerto (verde)
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(COR_LINHA1);
        for (int i = 0; i < n - 1; i++) {
            g.drawLine(xPts[i], yTaxa[i], xPts[i+1], yTaxa[i+1]);
        }
        // pontos taxa
        for (int i = 0; i < n; i++) {
            g.setColor(Color.WHITE);
            g.fillOval(xPts[i] - 5, yTaxa[i] - 5, 10, 10);
            g.setColor(COR_LINHA1);
            g.setStroke(new BasicStroke(2));
            g.drawOval(xPts[i] - 5, yTaxa[i] - 5, 10, 10);
        }

        // linha dificuldade (vermelho tracejado)
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 10, new float[]{8, 5}, 0));
        g.setColor(COR_LINHA2);
        for (int i = 0; i < n - 1; i++) {
            g.drawLine(xPts[i], yDif[i], xPts[i+1], yDif[i+1]);
        }
        g.setStroke(new BasicStroke(2));
        for (int i = 0; i < n; i++) {
            g.setColor(COR_LINHA2);
            g.fillOval(xPts[i] - 4, yDif[i] - 4, 9, 9);
        }

        // labels eixo X
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        for (int i = 0; i < n; i++) {
            String label = LABELS.getOrDefault(topicos.get(i),
                    topicos.get(i).replace("topico:", ""));
            Graphics2D gr = (Graphics2D) g.create();
            gr.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gr.translate(xPts[i], MT + areaH + 14);
            gr.rotate(Math.toRadians(35));
            gr.setColor(COR_TEXTO);
            gr.drawString(label, 0, 0);
            gr.dispose();
        }

        // legenda
        int ly = H - 28;
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(COR_LINHA1);
        g.drawLine(ML, ly - 4, ML + 24, ly - 4);
        g.setColor(COR_TEXTO);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString("Taxa de acerto (%)", ML + 30, ly);

        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 10, new float[]{8, 5}, 0));
        g.setColor(COR_LINHA2);
        g.drawLine(ML + 230, ly - 4, ML + 254, ly - 4);
        g.setColor(COR_TEXTO);
        g.setStroke(new BasicStroke(1));
        g.drawString("Dificuldade percebida (D(t))", ML + 260, ly);

        salvar(img, arquivo);
    }

    // ── utilitarios ─────────────────────────────────────────────

    // renderiza o grafico de dificuldade em um Graphics2D externo (usado pelo combinado)
    private static void renderizarDificuldade(Map<String, double[]> dados,
                                              Graphics2D g, int offX, int offY, int w, int h) {

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(offX, offY, w, h);

        List<String> topicos = new ArrayList<>(dados.keySet());
        int n = topicos.size();
        if (n == 0) return;

        int ml = offX + 60, mr = 20, mt = offY + 70, mb = 85;
        int areaW = w - ml + offX - mr - 60;
        int areaH = h - mt + offY - mb;

        // titulo
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(COR_TEXTO);
        String titulo = "Dificuldade percebida por t\u00f3pico";
        int tw = g.getFontMetrics().stringWidth(titulo);
        g.drawString(titulo, offX + w / 2 - tw / 2, offY + 24);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(COR_SUBTIT);
        String sub = "Agente Avaliador: 0,0 = dom\u00ednio, 1,0 = m\u00e1x. dificuldade";
        int sw = g.getFontMetrics().stringWidth(sub);
        g.drawString(sub, offX + w / 2 - sw / 2, offY + 42);

        // grade
        for (int v = 0; v <= 10; v += 2) {
            double val = v / 10.0;
            int y = mt + areaH - (int)(val * areaH);
            g.setColor(COR_GRADE);
            g.drawLine(ml, y, ml + areaW, y);
            g.setColor(COR_SUBTIT);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.drawString(String.format("%.1f", val).replace(".", ","), ml - 28, y + 4);
        }

        // limiar 0.7
        int yRef = mt + areaH - (int)(0.7 * areaH);
        g.setColor(new Color(216, 90, 48, 130));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10, new float[]{5, 4}, 0));
        g.drawLine(ml, yRef, ml + areaW, yRef);
        g.setStroke(new BasicStroke(1));
        g.setColor(COR_VERMELHO);
        g.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g.drawString("0,7", ml + areaW + 3, yRef + 4);

        g.setColor(new Color(200, 200, 200));
        g.drawLine(ml, mt, ml, mt + areaH);
        g.drawLine(ml, mt + areaH, ml + areaW, mt + areaH);

        int largBarra = Math.max(16, areaW / n - 10);
        int passo     = areaW / n;

        for (int i = 0; i < n; i++) {
            String topico = topicos.get(i);
            double dif    = dados.get(topico)[0];
            int x      = ml + i * passo + (passo - largBarra) / 2;
            int altBar = (int)(dif * areaH);
            int y      = mt + areaH - altBar;

            g.setColor(corPorDificuldade(dif));
            g.fillRoundRect(x, y, largBarra, Math.max(4, altBar), 5, 5);

            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            String val = String.format("%.2f", dif).replace(".", ",");
            int vw = g.getFontMetrics().stringWidth(val);
            g.setColor(COR_TEXTO);
            g.drawString(val, x + largBarra / 2 - vw / 2, y - 4);

            String label = LABELS.getOrDefault(topico, topico.replace("topico:", ""));
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            Graphics2D gr = (Graphics2D) g.create();
            gr.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gr.translate(x + largBarra / 2, mt + areaH + 12);
            gr.rotate(Math.toRadians(35));
            gr.setColor(COR_TEXTO);
            gr.drawString(label, 0, 0);
            gr.dispose();
        }

        // legenda
        int ly = offY + h - 26;
        desenharPilulaOff(g, ml, ly, COR_VERDE,    "< 0,4 \u2014 dom\u00ednio");
        desenharPilulaOff(g, ml + 140, ly, COR_AMARELO, "0,4\u20130,7");
        desenharPilulaOff(g, ml + 220, ly, COR_VERMELHO, "> 0,7 \u2014 dificuldade");
    }

    // renderiza o grafico de linha em um Graphics2D externo (usado pelo combinado)
    private static void renderizarLinha(Map<String, double[]> dados,
                                        Graphics2D g, int offX, int offY, int w, int h) {

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(offX, offY, w, h);

        List<String> topicos = new ArrayList<>(dados.keySet());
        int n = topicos.size();
        if (n == 0) return;

        int ml = offX + 60, mr = 20, mt = offY + 70, mb = 85;
        int areaW = w - ml + offX - mr - 20;
        int areaH = h - mt + offY - mb;

        // titulo
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(COR_TEXTO);
        String titulo = "Taxa de acerto vs. dificuldade percebida";
        int tw = g.getFontMetrics().stringWidth(titulo);
        g.drawString(titulo, offX + w / 2 - tw / 2, offY + 24);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(COR_SUBTIT);
        String sub = "Verde = acerto (%) · Vermelho tracejado = D(t) do Avaliador";
        int sw = g.getFontMetrics().stringWidth(sub);
        g.drawString(sub, offX + w / 2 - sw / 2, offY + 42);

        // grade
        for (int v = 0; v <= 10; v += 2) {
            double val = v / 10.0;
            int y = mt + areaH - (int)(val * areaH);
            g.setColor(COR_GRADE);
            g.drawLine(ml, y, ml + areaW, y);
            g.setColor(COR_SUBTIT);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.drawString(String.format("%.0f%%", val * 100), ml - 28, y + 4);
        }

        g.setColor(new Color(200, 200, 200));
        g.drawLine(ml, mt, ml, mt + areaH);
        g.drawLine(ml, mt + areaH, ml + areaW, mt + areaH);

        int passo = areaW / (n <= 1 ? 1 : n - 1);
        int[] xPts  = new int[n];
        int[] yTaxa = new int[n];
        int[] yDif  = new int[n];

        for (int i = 0; i < n; i++) {
            String topico = topicos.get(i);
            double[] d    = dados.get(topico);
            int acertos   = (int) d[1];
            int erros     = (int) d[2];
            int total     = acertos + erros;
            double taxa   = total == 0 ? 0 : acertos / (double) total;
            double dif    = d[0];
            xPts[i]  = ml + i * passo;
            yTaxa[i] = mt + areaH - (int)(taxa * areaH);
            yDif[i]  = mt + areaH - (int)(dif * areaH);
        }

        // linha verde (taxa)
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(COR_LINHA1);
        for (int i = 0; i < n - 1; i++) g.drawLine(xPts[i], yTaxa[i], xPts[i+1], yTaxa[i+1]);
        g.setStroke(new BasicStroke(2));
        for (int i = 0; i < n; i++) {
            g.setColor(Color.WHITE);
            g.fillOval(xPts[i]-4, yTaxa[i]-4, 9, 9);
            g.setColor(COR_LINHA1);
            g.drawOval(xPts[i]-4, yTaxa[i]-4, 9, 9);
        }

        // linha vermelha tracejada (dificuldade)
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 10, new float[]{7, 4}, 0));
        g.setColor(COR_LINHA2);
        for (int i = 0; i < n - 1; i++) g.drawLine(xPts[i], yDif[i], xPts[i+1], yDif[i+1]);
        g.setStroke(new BasicStroke(2));
        for (int i = 0; i < n; i++) {
            g.setColor(COR_LINHA2);
            g.fillOval(xPts[i]-4, yDif[i]-4, 8, 8);
        }

        // labels eixo X
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        for (int i = 0; i < n; i++) {
            String label = LABELS.getOrDefault(topicos.get(i),
                    topicos.get(i).replace("topico:", ""));
            Graphics2D gr = (Graphics2D) g.create();
            gr.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gr.translate(xPts[i], mt + areaH + 12);
            gr.rotate(Math.toRadians(35));
            gr.setColor(COR_TEXTO);
            gr.drawString(label, 0, 0);
            gr.dispose();
        }

        // legenda
        int ly = offY + h - 26;
        g.setStroke(new BasicStroke(2.2f));
        g.setColor(COR_LINHA1);
        g.drawLine(ml, ly - 4, ml + 20, ly - 4);
        g.setColor(COR_TEXTO);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.drawString("Taxa de acerto", ml + 25, ly);

        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 10, new float[]{7, 4}, 0));
        g.setColor(COR_LINHA2);
        g.drawLine(ml + 160, ly - 4, ml + 180, ly - 4);
        g.setColor(COR_TEXTO);
        g.setStroke(new BasicStroke(1));
        g.drawString("D(t) — dificuldade", ml + 185, ly);
    }

    private static void desenharPilulaOff(Graphics2D g, int x, int y, Color cor, String texto) {
        g.setColor(cor);
        g.fillRoundRect(x, y - 7, 10, 10, 3, 3);
        g.setColor(COR_SUBTIT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.drawString(texto, x + 14, y + 2);
    }

    private static Graphics2D configurar(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);
        return g;
    }

    private static void desenharTitulo(Graphics2D g, String titulo, String subtitulo) {
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(COR_TEXTO);
        int tw = g.getFontMetrics().stringWidth(titulo);
        g.drawString(titulo, W / 2 - tw / 2, 28);

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(COR_SUBTIT);
        int sw = g.getFontMetrics().stringWidth(subtitulo);
        g.drawString(subtitulo, W / 2 - sw / 2, 46);
    }

    private static void desenharPilula(Graphics2D g, int x, int y, Color cor, String texto) {
        g.setColor(cor);
        g.fillRoundRect(x, y - 8, 12, 12, 4, 4);
        g.setColor(COR_SUBTIT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString(texto, x + 16, y + 2);
    }

    private static Color corPorTaxa(double taxa) {
        if (taxa >= 70) return COR_VERDE;
        if (taxa >= 40) return COR_AMARELO;
        return COR_VERMELHO;
    }

    private static Color corPorDificuldade(double dif) {
        if (dif >= 0.7) return COR_VERMELHO;
        if (dif >= 0.4) return COR_AMARELO;
        return COR_VERDE;
    }

    private static void salvar(BufferedImage img, String arquivo) throws Exception {
        ImageIO.write(img, "PNG", new File(arquivo));
    }
}