package br.edu.linguagens.esportes.dominio;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Identificador nominal dos formatos aceitos pelo pipeline.
 */
public enum FormatoEstatistica {
    PERCENTUAL,
    FRACAO,
    DECIMAL,
    AUTOMATICO;

    public static FormatoEstatistica de(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("O formato da estatistica e obrigatorio.");
        }

        String identificador = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (identificador) {
            case "PERCENTUAL", "PORCENTAGEM", "PERCENTAGE", "PERCENT" -> PERCENTUAL;
            case "FRACAO", "FRACAO_NUMERICA", "FRACTION", "RATIO" -> FRACAO;
            case "DECIMAL", "TAXA_DECIMAL" -> DECIMAL;
            case "AUTOMATICO", "AUTO", "AUTOMATIC" -> AUTOMATICO;
            default -> throw new IllegalArgumentException(
                    "Formato invalido: '" + texto
                            + "'. Use PERCENTUAL, FRACAO, DECIMAL ou AUTOMATICO."
            );
        };
    }
}
