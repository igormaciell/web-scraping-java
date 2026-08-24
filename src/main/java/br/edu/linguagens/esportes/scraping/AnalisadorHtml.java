package br.edu.linguagens.esportes.scraping;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Converte HTML textual em uma arvore simples, sem depender de bibliotecas externas. */
final class AnalisadorHtml {

    NoHtml analisar(String html) throws IOException {
        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException("O documento HTML esta vazio.");
        }

        NoHtml raiz = new NoHtml("document", Map.of(), null);
        Deque<NoHtml> pilha = new ArrayDeque<>();
        pilha.push(raiz);

        HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {
            @Override
            public void handleStartTag(HTML.Tag tag, MutableAttributeSet atributos, int posicao) {
                NoHtml no = criarNo(tag, atributos, pilha.peek());
                pilha.peek().adicionarFilho(no);
                pilha.push(no);
            }

            @Override
            public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet atributos, int posicao) {
                NoHtml no = criarNo(tag, atributos, pilha.peek());
                pilha.peek().adicionarFilho(no);
            }

            @Override
            public void handleEndTag(HTML.Tag tag, int posicao) {
                String nomeTag = tag.toString().toLowerCase(Locale.ROOT);
                while (pilha.size() > 1) {
                    NoHtml removido = pilha.pop();
                    if (removido.tag().equals(nomeTag)) {
                        break;
                    }
                }
            }

            @Override
            public void handleText(char[] dados, int posicao) {
                pilha.peek().adicionarTexto(new String(dados));
            }
        };

        new ParserDelegator().parse(new StringReader(html), callback, true);
        return raiz;
    }

    private static NoHtml criarNo(HTML.Tag tag, AttributeSet atributos, NoHtml pai) {
        return new NoHtml(tag.toString(), converterAtributos(atributos), pai);
    }

    private static Map<String, String> converterAtributos(AttributeSet atributos) {
        Map<String, String> resultado = new LinkedHashMap<>();
        Enumeration<?> nomes = atributos.getAttributeNames();

        while (nomes.hasMoreElements()) {
            Object nomeOriginal = nomes.nextElement();
            if (StyleConstants.NameAttribute.equals(nomeOriginal)) {
                continue;
            }

            Object valorOriginal = atributos.getAttribute(nomeOriginal);
            if (valorOriginal == null) {
                continue;
            }

            String nome = nomeOriginal.toString().toLowerCase(Locale.ROOT);
            resultado.put(nome, valorOriginal.toString());
        }
        return resultado;
    }
}
