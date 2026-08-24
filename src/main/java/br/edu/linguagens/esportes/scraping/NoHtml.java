package br.edu.linguagens.esportes.scraping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Representacao minima de um no HTML, suficiente para a selecao usada no trabalho. */
final class NoHtml {
    private final String tag;
    private final Map<String, String> atributos;
    private final NoHtml pai;
    private final List<NoHtml> filhos = new ArrayList<>();
    private final List<Object> conteudoOrdenado = new ArrayList<>();

    NoHtml(String tag, Map<String, String> atributos, NoHtml pai) {
        this.tag = tag.toLowerCase(Locale.ROOT);
        this.atributos = Collections.unmodifiableMap(new LinkedHashMap<>(atributos));
        this.pai = pai;
    }

    String tag() {
        return tag;
    }

    NoHtml pai() {
        return pai;
    }

    List<NoHtml> filhos() {
        return Collections.unmodifiableList(filhos);
    }

    String atributo(String nome) {
        return atributos.get(nome.toLowerCase(Locale.ROOT));
    }

    boolean possuiAtributo(String nome) {
        return atributos.containsKey(nome.toLowerCase(Locale.ROOT));
    }

    void adicionarFilho(NoHtml filho) {
        filhos.add(filho);
        conteudoOrdenado.add(filho);
    }

    void adicionarTexto(String texto) {
        if (texto != null && !texto.isEmpty()) {
            conteudoOrdenado.add(texto);
        }
    }

    String textoCompleto() {
        StringBuilder texto = new StringBuilder();
        anexarTexto(texto);
        return normalizarEspacos(texto.toString());
    }

    private void anexarTexto(StringBuilder destino) {
        for (Object item : conteudoOrdenado) {
            if (item instanceof String trecho) {
                destino.append(trecho).append(' ');
            } else if (item instanceof NoHtml filho) {
                filho.anexarTexto(destino);
            }
        }
    }

    private static String normalizarEspacos(String texto) {
        return texto.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }
}
