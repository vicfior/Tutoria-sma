package br.iesb.tutoria.mensagens;

public class TiposMensagem {
    public static final String ALERTA    = "ALERTA";
    public static final String LACUNA    = "LACUNA";
    public static final String SOLICITA  = "SOLICITA";
    public static final String ENTREGA   = "ENTREGA";
    public static final String RELATORIO = "RELATÓRIO";
    public static final String EXECUTA   = "EXECUTA";

    /** Extrai campo por índice (separador |) */
    public static String campo(String msg, int idx) {
        String[] p = msg.split("\\|");
        return idx < p.length ? p[idx] : "";
    }
}