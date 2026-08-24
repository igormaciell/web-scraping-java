package br.edu.linguagens.esportes.scraping;

import java.io.IOException;

/** Localiza o primeiro elemento correspondente ao seletor e devolve seu texto visivel. */
public final class ExtratorHtml {
    private final ClienteHttpHtml clienteHttp;
    private final AnalisadorHtml analisadorHtml;
    private final SeletorCssSimples seletorCss;

    public ExtratorHtml() {
        this.clienteHttp = new ClienteHttpHtml();
        this.analisadorHtml = new AnalisadorHtml();
        this.seletorCss = new SeletorCssSimples();
    }

    public ConteudoExtraido extrairDeUrl(String url, String seletor) throws IOException {
        ClienteHttpHtml.DocumentoRemoto documento = clienteHttp.buscar(url);
        String texto = extrairDeHtml(documento.html(), seletor);
        return new ConteudoExtraido(texto, documento.urlFinal());
    }

    public String extrairDeHtml(String html, String seletor) throws IOException {
        NoHtml raiz = analisadorHtml.analisar(html);
        NoHtml elemento = seletorCss.selecionarPrimeiro(raiz, seletor);
        if (elemento == null) {
            throw new IllegalArgumentException(
                    "Nenhum elemento foi encontrado para o seletor CSS '" + seletor + "'."
            );
        }

        String texto = elemento.textoCompleto();
        if (texto.isBlank()) {
            throw new IllegalArgumentException(
                    "O elemento encontrado pelo seletor '" + seletor + "' nao possui texto."
            );
        }
        return texto;
    }

    public record ConteudoExtraido(String texto, String fonteFinal) {
    }
}
