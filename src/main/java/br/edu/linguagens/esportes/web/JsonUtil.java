package br.edu.linguagens.esportes.web;

import br.edu.linguagens.esportes.dominio.FormatoEstatistica;
import br.edu.linguagens.esportes.dominio.ResultadoEstatistica;

import java.math.BigDecimal;
import java.util.List;

final class JsonUtil {
    private JsonUtil() {
    }

    static String resultado(ResultadoEstatistica resultado) {
        return "{" +
                campo("esporte", resultado.esporte()) + "," +
                campo("atleta", resultado.atleta()) + "," +
                campo("metrica", resultado.metrica()) + "," +
                campo("valorOriginal", resultado.valorOriginal()) + "," +
                "\"valorNormalizado\":" + numero(resultado.valorNormalizado()) + "," +
                campo("formato", resultado.formato().name()) + "," +
                campo("fonte", resultado.fonte()) + "," +
                campo("coletadoEm", resultado.coletadoEm().toString()) +
                "}";
    }

    static String resultados(List<ResultadoEstatistica> resultados) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"quantidade\":").append(resultados.size()).append(',');
        json.append("\"resultados\":[");
        for (int i = 0; i < resultados.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(resultado(resultados.get(i)));
        }
        return json.append("]}").toString();
    }

    static String normalizacao(
            String valorOriginal,
            FormatoEstatistica formato,
            double valorNormalizado
    ) {
        return "{" +
                campo("valorOriginal", valorOriginal) + "," +
                campo("formato", formato.name()) + "," +
                "\"valorNormalizado\":" + numero(valorNormalizado) +
                "}";
    }

    static String erro(String tipo, String mensagem) {
        return "{" + campo("erro", tipo) + "," + campo("mensagem", mensagem) + "}";
    }

    static String saude(int porta) {
        return "{" +
                campo("status", "ok") + "," +
                campo("aplicacao", "Extrator de Estatisticas Esportivas") + "," +
                "\"porta\":" + porta +
                "}";
    }

    private static String campo(String nome, String valor) {
        return "\"" + escapar(nome) + "\":\"" + escapar(valor) + "\"";
    }

    private static String numero(double valor) {
        String texto = BigDecimal.valueOf(valor).stripTrailingZeros().toPlainString();
        return texto.contains(".") ? texto : texto + ".0";
    }

    private static String escapar(String texto) {
        if (texto == null) {
            return "";
        }

        StringBuilder resultado = new StringBuilder(texto.length() + 16);
        for (int i = 0; i < texto.length(); i++) {
            char caractere = texto.charAt(i);
            switch (caractere) {
                case '"' -> resultado.append("\\\"");
                case '\\' -> resultado.append("\\\\");
                case '\b' -> resultado.append("\\b");
                case '\f' -> resultado.append("\\f");
                case '\n' -> resultado.append("\\n");
                case '\r' -> resultado.append("\\r");
                case '\t' -> resultado.append("\\t");
                default -> {
                    if (caractere < 0x20) {
                        resultado.append(String.format("\\u%04x", (int) caractere));
                    } else {
                        resultado.append(caractere);
                    }
                }
            }
        }
        return resultado.toString();
    }
}
