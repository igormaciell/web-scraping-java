package br.edu.linguagens.esportes.scraping;

import br.edu.linguagens.esportes.dominio.FormatoEstatistica;
import br.edu.linguagens.esportes.dominio.ResultadoEstatistica;
import br.edu.linguagens.esportes.dominio.SolicitacaoExtracao;
import br.edu.linguagens.esportes.normalizacao.Normalizadores;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;

/** Orquestra coleta, extracao, normalizacao e criacao da resposta padronizada. */
public final class PipelineScraping {
    private final ExtratorHtml extratorHtml;
    private final Normalizadores normalizadores;

    public PipelineScraping() {
        this(new ExtratorHtml(), new Normalizadores());
    }

    PipelineScraping(ExtratorHtml extratorHtml, Normalizadores normalizadores) {
        this.extratorHtml = Objects.requireNonNull(extratorHtml);
        this.normalizadores = Objects.requireNonNull(normalizadores);
    }

    public ResultadoEstatistica executar(SolicitacaoExtracao solicitacao) throws IOException {
        ExtratorHtml.ConteudoExtraido conteudo = extratorHtml.extrairDeUrl(
                solicitacao.url(),
                solicitacao.seletorCss()
        );

        // A funcao e obtida do registro e tratada como um valor de primeira classe.
        Function<String, Double> normalizador = normalizadores.obter(solicitacao.formato());
        return criarResultado(
                solicitacao,
                conteudo.texto(),
                conteudo.fonteFinal(),
                normalizador
        );
    }

    public ResultadoEstatistica executarComHtml(
            SolicitacaoExtracao solicitacao,
            String html
    ) throws IOException {
        String valorOriginal = extratorHtml.extrairDeHtml(html, solicitacao.seletorCss());
        Function<String, Double> normalizador = normalizadores.obter(solicitacao.formato());
        return criarResultado(solicitacao, valorOriginal, solicitacao.url(), normalizador);
    }

    public double normalizarDiretamente(String valorOriginal, FormatoEstatistica formato) {
        return normalizadores.normalizar(valorOriginal, formato);
    }

    public double normalizarComFuncaoCustomizada(
            String valorOriginal,
            Function<String, Double> normalizador
    ) {
        return normalizadores.aplicar(valorOriginal, normalizador);
    }

    /**
     * Funcao de ordem superior: recebe o comportamento de normalizacao como argumento.
     */
    private ResultadoEstatistica criarResultado(
            SolicitacaoExtracao solicitacao,
            String valorOriginal,
            String fonteFinal,
            Function<String, Double> normalizador
    ) {
        double valorNormalizado = normalizadores.aplicar(valorOriginal, normalizador);
        return new ResultadoEstatistica(
                solicitacao.esporte(),
                solicitacao.atleta(),
                solicitacao.metrica(),
                valorOriginal,
                valorNormalizado,
                solicitacao.formato(),
                fonteFinal,
                Instant.now()
        );
    }
}
