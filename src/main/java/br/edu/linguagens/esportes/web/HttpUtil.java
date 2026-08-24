package br.edu.linguagens.esportes.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class HttpUtil {
    private HttpUtil() {
    }

    static boolean prepararGet(HttpExchange exchange) throws IOException {
        adicionarCabecalhosComuns(exchange.getResponseHeaders());
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return false;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET, OPTIONS");
            responderJson(
                    exchange,
                    405,
                    JsonUtil.erro("metodo_nao_permitido", "Este endpoint aceita somente GET.")
            );
            return false;
        }
        return true;
    }

    static Map<String, String> parametrosConsulta(HttpExchange exchange) {
        Map<String, String> parametros = new LinkedHashMap<>();
        String consulta = exchange.getRequestURI().getRawQuery();
        if (consulta == null || consulta.isBlank()) {
            return parametros;
        }

        for (String par : consulta.split("&")) {
            if (par.isEmpty()) {
                continue;
            }
            int separador = par.indexOf('=');
            String chaveBruta = separador >= 0 ? par.substring(0, separador) : par;
            String valorBruto = separador >= 0 ? par.substring(separador + 1) : "";
            String chave = URLDecoder.decode(chaveBruta, StandardCharsets.UTF_8);
            String valor = URLDecoder.decode(valorBruto, StandardCharsets.UTF_8);
            parametros.put(chave, valor);
        }
        return parametros;
    }

    static String obrigatorio(Map<String, String> parametros, String nome) {
        String valor = parametros.get(nome);
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O parametro '" + nome + "' e obrigatorio.");
        }
        return valor.trim();
    }

    static String opcional(Map<String, String> parametros, String nome, String padrao) {
        String valor = parametros.get(nome);
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }

    static void responderJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] corpo = json.getBytes(StandardCharsets.UTF_8);
        Headers cabecalhos = exchange.getResponseHeaders();
        adicionarCabecalhosComuns(cabecalhos);
        cabecalhos.set("Content-Type", "application/json; charset=UTF-8");
        cabecalhos.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, corpo.length);
        try (OutputStream saida = exchange.getResponseBody()) {
            saida.write(corpo);
        }
    }

    static void responderBytes(
            HttpExchange exchange,
            int status,
            byte[] corpo,
            String contentType,
            boolean somenteCabecalhos
    ) throws IOException {
        Headers cabecalhos = exchange.getResponseHeaders();
        adicionarCabecalhosComuns(cabecalhos);
        cabecalhos.set("Content-Type", contentType);
        cabecalhos.set("Cache-Control", "no-cache");
        if (somenteCabecalhos) {
            cabecalhos.set("Content-Length", Integer.toString(corpo.length));
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(status, corpo.length);
        try (OutputStream saida = exchange.getResponseBody()) {
            saida.write(corpo);
        }
    }

    static void adicionarCabecalhosComuns(Headers cabecalhos) {
        cabecalhos.set("Access-Control-Allow-Origin", "*");
        cabecalhos.set("Access-Control-Allow-Methods", "GET, OPTIONS");
        cabecalhos.set("Access-Control-Allow-Headers", "Content-Type");
        cabecalhos.set("X-Content-Type-Options", "nosniff");
        cabecalhos.set("Referrer-Policy", "no-referrer");
    }
}
