package br.edu.linguagens.esportes.web;

import br.edu.linguagens.esportes.dominio.FormatoEstatistica;
import br.edu.linguagens.esportes.dominio.ResultadoEstatistica;
import br.edu.linguagens.esportes.dominio.SolicitacaoExtracao;
import br.edu.linguagens.esportes.scraping.PipelineScraping;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Camada Java Web: publica a interface e os endpoints REST. */
public final class ServidorWeb {
    private final int porta;
    private final PipelineScraping pipeline;
    private HttpServer servidor;
    private ExecutorService executor;

    public ServidorWeb(int porta) {
        if (porta < 1 || porta > 65_535) {
            throw new IllegalArgumentException("A porta deve estar entre 1 e 65535.");
        }
        this.porta = porta;
        this.pipeline = new PipelineScraping();
    }

    public void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        servidor.createContext("/api/saude", this::tratarSaude);
        servidor.createContext("/api/normalizar", this::tratarNormalizacao);
        servidor.createContext("/api/raspar", this::tratarScraping);
        servidor.createContext("/api/demonstracao", this::tratarDemonstracao);
        servidor.createContext("/", this::tratarRecursoEstatico);

        executor = Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
        servidor.setExecutor(executor);
        servidor.start();
    }

    public void parar() {
        if (servidor != null) {
            servidor.stop(1);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void tratarSaude(HttpExchange exchange) throws IOException {
        if (!HttpUtil.prepararGet(exchange)) {
            return;
        }
        HttpUtil.responderJson(exchange, 200, JsonUtil.saude(porta));
    }

    private void tratarNormalizacao(HttpExchange exchange) throws IOException {
        if (!HttpUtil.prepararGet(exchange)) {
            return;
        }

        try {
            Map<String, String> parametros = HttpUtil.parametrosConsulta(exchange);
            String valorOriginal = HttpUtil.obrigatorio(parametros, "valor");
            FormatoEstatistica formato = FormatoEstatistica.de(
                    HttpUtil.obrigatorio(parametros, "formato")
            );
            double valorNormalizado = pipeline.normalizarDiretamente(valorOriginal, formato);
            HttpUtil.responderJson(
                    exchange,
                    200,
                    JsonUtil.normalizacao(valorOriginal, formato, valorNormalizado)
            );
        } catch (IllegalArgumentException erro) {
            HttpUtil.responderJson(
                    exchange,
                    400,
                    JsonUtil.erro("requisicao_invalida", mensagemSegura(erro))
            );
        } catch (Exception erro) {
            HttpUtil.responderJson(
                    exchange,
                    500,
                    JsonUtil.erro("erro_interno", mensagemSegura(erro))
            );
        }
    }

    private void tratarScraping(HttpExchange exchange) throws IOException {
        if (!HttpUtil.prepararGet(exchange)) {
            return;
        }

        try {
            Map<String, String> parametros = HttpUtil.parametrosConsulta(exchange);
            SolicitacaoExtracao solicitacao = new SolicitacaoExtracao(
                    HttpUtil.opcional(parametros, "esporte", "Nao informado"),
                    HttpUtil.opcional(parametros, "atleta", "Nao informado"),
                    HttpUtil.opcional(parametros, "metrica", "Estatistica"),
                    HttpUtil.obrigatorio(parametros, "url"),
                    HttpUtil.obrigatorio(parametros, "seletor"),
                    FormatoEstatistica.de(HttpUtil.obrigatorio(parametros, "formato"))
            );

            ResultadoEstatistica resultado = pipeline.executar(solicitacao);
            HttpUtil.responderJson(exchange, 200, JsonUtil.resultado(resultado));
        } catch (IllegalArgumentException erro) {
            HttpUtil.responderJson(
                    exchange,
                    400,
                    JsonUtil.erro("requisicao_invalida", mensagemSegura(erro))
            );
        } catch (IOException erro) {
            HttpUtil.responderJson(
                    exchange,
                    502,
                    JsonUtil.erro("falha_na_coleta", mensagemSegura(erro))
            );
        } catch (Exception erro) {
            HttpUtil.responderJson(
                    exchange,
                    500,
                    JsonUtil.erro("erro_interno", mensagemSegura(erro))
            );
        }
    }

    private void tratarDemonstracao(HttpExchange exchange) throws IOException {
        if (!HttpUtil.prepararGet(exchange)) {
            return;
        }

        try {
            List<ResultadoEstatistica> resultados = new ArrayList<>();
            String baseLocal = "http://localhost:" + porta + "/examples/";

            resultados.add(executarExemplo(
                    "Futebol",
                    "Equipe Aurora",
                    "Posse de bola",
                    baseLocal + "futebol.html",
                    "div.cartao-jogador .posse-bola",
                    FormatoEstatistica.PERCENTUAL,
                    "/static/examples/futebol.html"
            ));
            resultados.add(executarExemplo(
                    "Futebol",
                    "Equipe Aurora",
                    "Taxa de passes certos",
                    baseLocal + "futebol.html",
                    "#taxa-passes",
                    FormatoEstatistica.PERCENTUAL,
                    "/static/examples/futebol.html"
            ));
            resultados.add(executarExemplo(
                    "Basquete",
                    "Marina Costa",
                    "Eficiencia de arremessos",
                    baseLocal + "basquete.html",
                    "[data-estatistica='arremessos']",
                    FormatoEstatistica.FRACAO,
                    "/static/examples/basquete.html"
            ));
            resultados.add(executarExemplo(
                    "Volei",
                    "Lucas Nunes",
                    "Aproveitamento de saques",
                    baseLocal + "volei.html",
                    "div.atleta .eficiencia",
                    FormatoEstatistica.DECIMAL,
                    "/static/examples/volei.html"
            ));

            HttpUtil.responderJson(exchange, 200, JsonUtil.resultados(resultados));
        } catch (IllegalArgumentException erro) {
            HttpUtil.responderJson(
                    exchange,
                    400,
                    JsonUtil.erro("requisicao_invalida", mensagemSegura(erro))
            );
        } catch (Exception erro) {
            HttpUtil.responderJson(
                    exchange,
                    500,
                    JsonUtil.erro("erro_interno", mensagemSegura(erro))
            );
        }
    }

    private ResultadoEstatistica executarExemplo(
            String esporte,
            String atleta,
            String metrica,
            String url,
            String seletor,
            FormatoEstatistica formato,
            String recurso
    ) throws IOException {
        String html = new String(carregarRecurso(recurso), StandardCharsets.UTF_8);
        SolicitacaoExtracao solicitacao = new SolicitacaoExtracao(
                esporte,
                atleta,
                metrica,
                url,
                seletor,
                formato
        );
        return pipeline.executarComHtml(solicitacao, html);
    }

    private void tratarRecursoEstatico(HttpExchange exchange) throws IOException {
        HttpUtil.adicionarCabecalhosComuns(exchange.getResponseHeaders());
        String metodo = exchange.getRequestMethod();
        boolean apenasCabecalhos = "HEAD".equalsIgnoreCase(metodo);
        if (!("GET".equalsIgnoreCase(metodo) || apenasCabecalhos)) {
            exchange.getResponseHeaders().set("Allow", "GET, HEAD");
            HttpUtil.responderBytes(
                    exchange,
                    405,
                    "Metodo nao permitido.".getBytes(StandardCharsets.UTF_8),
                    "text/plain; charset=UTF-8",
                    false
            );
            return;
        }

        String caminho = exchange.getRequestURI().getPath();
        if (caminho == null || caminho.isBlank() || "/".equals(caminho)) {
            caminho = "/index.html";
        }
        if (caminho.contains("..") || caminho.contains("\\")) {
            responderNaoEncontrado(exchange, apenasCabecalhos);
            return;
        }

        String recurso = "/static" + caminho;
        byte[] conteudo;
        try {
            conteudo = carregarRecurso(recurso);
        } catch (IOException erro) {
            responderNaoEncontrado(exchange, apenasCabecalhos);
            return;
        }

        HttpUtil.responderBytes(
                exchange,
                200,
                conteudo,
                descobrirContentType(caminho),
                apenasCabecalhos
        );
    }

    private void responderNaoEncontrado(HttpExchange exchange, boolean apenasCabecalhos) throws IOException {
        HttpUtil.responderBytes(
                exchange,
                404,
                "Recurso nao encontrado.".getBytes(StandardCharsets.UTF_8),
                "text/plain; charset=UTF-8",
                apenasCabecalhos
        );
    }

    private byte[] carregarRecurso(String caminho) throws IOException {
        try (InputStream entrada = ServidorWeb.class.getResourceAsStream(caminho)) {
            if (entrada == null) {
                throw new IOException("Recurso interno nao encontrado: " + caminho);
            }
            return entrada.readAllBytes();
        }
    }

    private static String descobrirContentType(String caminho) {
        String caminhoMinusculo = caminho.toLowerCase(Locale.ROOT);
        if (caminhoMinusculo.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (caminhoMinusculo.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (caminhoMinusculo.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        if (caminhoMinusculo.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        }
        if (caminhoMinusculo.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    private static String mensagemSegura(Throwable erro) {
        String mensagem = erro.getMessage();
        return mensagem == null || mensagem.isBlank()
                ? erro.getClass().getSimpleName()
                : mensagem;
    }
}
