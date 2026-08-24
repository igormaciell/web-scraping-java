package br.edu.linguagens.esportes.scraping;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Realiza a coleta HTTP no momento de cada requisicao do usuario. */
final class ClienteHttpHtml {
    private static final int LIMITE_MAXIMO_HTML_BYTES = 2_000_000;
    private static final Duration TEMPO_LIMITE_REQUISICAO = Duration.ofSeconds(12);
    private static final Pattern PADRAO_CHARSET = Pattern.compile(
            "charset\\s*=\\s*[\"']?([^;\"'\\s]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final String AGENTE_USUARIO =
            "ExtratorEstatisticasAcademico/1.0 (+Java-Web-Scraping)";

    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    DocumentoRemoto buscar(String endereco) throws IOException {
        URI uri = validarUri(endereco);
        HttpRequest requisicao = HttpRequest.newBuilder(uri)
                .timeout(TEMPO_LIMITE_REQUISICAO)
                .header("User-Agent", AGENTE_USUARIO)
                .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.1")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> resposta = cliente.send(
                    requisicao,
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            int status = resposta.statusCode();
            if (status < 200 || status >= 300) {
                throw new IOException(
                        "O portal respondeu com status HTTP " + status + " para " + resposta.uri() + "."
                );
            }

            byte[] corpo = resposta.body();
            if (corpo.length > LIMITE_MAXIMO_HTML_BYTES) {
                throw new IOException(
                        "O HTML excede o limite didatico de " + LIMITE_MAXIMO_HTML_BYTES + " bytes."
                );
            }

            Charset charset = descobrirCharset(
                    resposta.headers().firstValue("Content-Type").orElse("")
            );
            return new DocumentoRemoto(new String(corpo, charset), resposta.uri().toString());
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
            throw new IOException("A coleta HTTP foi interrompida.", erro);
        } catch (IllegalArgumentException erro) {
            throw erro;
        }
    }

    private static URI validarUri(String endereco) {
        try {
            URI uri = URI.create(endereco.trim());
            String esquema = uri.getScheme();
            if (esquema == null
                    || !(esquema.equalsIgnoreCase("http") || esquema.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("A URL deve usar o protocolo http ou https.");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("A URL deve possuir um host valido.");
            }
            if (uri.getUserInfo() != null) {
                throw new IllegalArgumentException("URLs com usuario e senha nao sao aceitas.");
            }
            return uri;
        } catch (IllegalArgumentException erro) {
            if (erro.getMessage() != null && erro.getMessage().startsWith("A URL")) {
                throw erro;
            }
            throw new IllegalArgumentException("URL invalida: '" + endereco + "'.", erro);
        }
    }

    private static Charset descobrirCharset(String contentType) {
        Matcher matcher = PADRAO_CHARSET.matcher(contentType);
        if (matcher.find()) {
            try {
                return Charset.forName(matcher.group(1).trim());
            } catch (Exception ignorado) {
                // Se o servidor informar um charset desconhecido, usa UTF-8 como alternativa segura.
            }
        }

        String tipoNormalizado = contentType.toLowerCase(Locale.ROOT);
        if (tipoNormalizado.contains("iso-8859-1")) {
            return StandardCharsets.ISO_8859_1;
        }
        return StandardCharsets.UTF_8;
    }

    record DocumentoRemoto(String html, String urlFinal) {
    }
}
