package br.valueobject;

import java.text.Normalizer;
import java.util.Locale;

// agrupa segmento e subsegmento sem criar outra tabela
public final class PerfilComercial {
    private final String segmento;
    private final String subsegmento;

    public PerfilComercial(String segmento, String subsegmento) {
        this.segmento = normalizar(segmento);
        this.subsegmento = normalizar(subsegmento);
    }

    public String getSegmento() {
        return segmento;
    }

    public String getSubsegmento() {
        return subsegmento;
    }

    public boolean ehAtacado() {
        return segmento.contains("atacado");
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().toLowerCase(Locale.ROOT);
    }
}
