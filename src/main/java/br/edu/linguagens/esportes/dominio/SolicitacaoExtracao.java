package br.edu.linguagens.esportes.dominio;

/**
 * Dados necessarios para localizar e interpretar uma estatistica em um portal.
 */
public record SolicitacaoExtracao(
        String esporte,
        String atleta,
        String metrica,
        String url,
        String seletorCss,
        FormatoEstatistica formato
) {
    private static final int TAMANHO_MAXIMO_CAMPO = 2_000;

    public SolicitacaoExtracao {
        esporte = validarTexto(esporte, "esporte");
        atleta = validarTexto(atleta, "atleta");
        metrica = validarTexto(metrica, "metrica");
        url = validarTexto(url, "url");
        seletorCss = validarTexto(seletorCss, "seletorCss");

        if (formato == null) {
            throw new IllegalArgumentException("O formato da estatistica e obrigatorio.");
        }
    }

    private static String validarTexto(String valor, String nomeCampo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O campo '" + nomeCampo + "' e obrigatorio.");
        }

        String textoLimpo = valor.trim();
        if (textoLimpo.length() > TAMANHO_MAXIMO_CAMPO) {
            throw new IllegalArgumentException(
                    "O campo '" + nomeCampo + "' excedeu " + TAMANHO_MAXIMO_CAMPO + " caracteres."
            );
        }
        return textoLimpo;
    }
}
