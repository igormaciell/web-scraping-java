package br.edu.linguagens.esportes.scraping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Implementa um subconjunto didatico de seletores CSS:
 * tag, #id, .classe, tag.classe, [atributo], [atributo=valor] e descendentes.
 */
final class SeletorCssSimples {

    NoHtml selecionarPrimeiro(NoHtml raiz, String seletorCss) {
        List<SeletorElemento> cadeia = analisarSeletor(seletorCss);
        return buscarEmProfundidade(raiz, cadeia);
    }

    private NoHtml buscarEmProfundidade(NoHtml atual, List<SeletorElemento> cadeia) {
        for (NoHtml filho : atual.filhos()) {
            if (correspondeCadeia(filho, cadeia)) {
                return filho;
            }
            NoHtml encontrado = buscarEmProfundidade(filho, cadeia);
            if (encontrado != null) {
                return encontrado;
            }
        }
        return null;
    }

    private boolean correspondeCadeia(NoHtml candidato, List<SeletorElemento> cadeia) {
        int indice = cadeia.size() - 1;
        if (!cadeia.get(indice).corresponde(candidato)) {
            return false;
        }

        NoHtml ancestral = candidato.pai();
        indice--;
        while (indice >= 0) {
            while (ancestral != null && !cadeia.get(indice).corresponde(ancestral)) {
                ancestral = ancestral.pai();
            }
            if (ancestral == null) {
                return false;
            }
            ancestral = ancestral.pai();
            indice--;
        }
        return true;
    }

    private List<SeletorElemento> analisarSeletor(String seletorCss) {
        if (seletorCss == null || seletorCss.isBlank()) {
            throw new IllegalArgumentException("O seletor CSS e obrigatorio.");
        }

        if (contemCombinadorNaoSuportado(seletorCss)) {
            throw new IllegalArgumentException(
                    "Seletor nao suportado. Use tag, #id, .classe, [atributo=valor] "
                            + "e combinacao por espaco (descendente)."
            );
        }

        List<String> partes = separarPorEspacos(seletorCss.trim());
        List<SeletorElemento> resultado = new ArrayList<>();
        for (String parte : partes) {
            resultado.add(SeletorElemento.analisar(parte));
        }
        return resultado;
    }

    private boolean contemCombinadorNaoSuportado(String seletor) {
        boolean emColchetes = false;
        char aspas = 0;
        for (int i = 0; i < seletor.length(); i++) {
            char caractere = seletor.charAt(i);
            if ((caractere == '\'' || caractere == '"') && emColchetes) {
                if (aspas == 0) {
                    aspas = caractere;
                } else if (aspas == caractere) {
                    aspas = 0;
                }
                continue;
            }
            if (aspas != 0) {
                continue;
            }
            if (caractere == '[') {
                emColchetes = true;
            } else if (caractere == ']') {
                emColchetes = false;
            } else if (!emColchetes && (caractere == '>' || caractere == '+'
                    || caractere == '~' || caractere == ',' || caractere == ':')) {
                return true;
            }
        }
        return false;
    }

    private List<String> separarPorEspacos(String seletor) {
        List<String> partes = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean emColchetes = false;
        char aspas = 0;

        for (int i = 0; i < seletor.length(); i++) {
            char caractere = seletor.charAt(i);
            if ((caractere == '\'' || caractere == '"') && emColchetes) {
                if (aspas == 0) {
                    aspas = caractere;
                } else if (aspas == caractere) {
                    aspas = 0;
                }
                atual.append(caractere);
                continue;
            }
            if (aspas == 0) {
                if (caractere == '[') {
                    emColchetes = true;
                } else if (caractere == ']') {
                    emColchetes = false;
                } else if (Character.isWhitespace(caractere) && !emColchetes) {
                    if (!atual.isEmpty()) {
                        partes.add(atual.toString());
                        atual.setLength(0);
                    }
                    continue;
                }
            }
            atual.append(caractere);
        }

        if (emColchetes || aspas != 0) {
            throw new IllegalArgumentException("Seletor CSS com colchetes ou aspas nao fechados.");
        }
        if (!atual.isEmpty()) {
            partes.add(atual.toString());
        }
        if (partes.isEmpty()) {
            throw new IllegalArgumentException("O seletor CSS e obrigatorio.");
        }
        return partes;
    }

    private record RestricaoAtributo(String nome, String valorEsperado, boolean exigeValor) {
        boolean corresponde(NoHtml no) {
            if (!no.possuiAtributo(nome)) {
                return false;
            }
            return !exigeValor || valorEsperado.equals(no.atributo(nome));
        }
    }

    private record SeletorElemento(
            String tag,
            String id,
            Set<String> classes,
            List<RestricaoAtributo> atributos
    ) {
        static SeletorElemento analisar(String texto) {
            String tag = null;
            String id = null;
            Set<String> classes = new HashSet<>();
            List<RestricaoAtributo> atributos = new ArrayList<>();

            int indice = 0;
            if (indice < texto.length() && texto.charAt(indice) == '*') {
                tag = "*";
                indice++;
            } else if (indice < texto.length() && eInicioIdentificador(texto.charAt(indice))) {
                int inicio = indice;
                indice = avancarIdentificador(texto, indice);
                tag = texto.substring(inicio, indice).toLowerCase(Locale.ROOT);
            }

            while (indice < texto.length()) {
                char caractere = texto.charAt(indice);
                if (caractere == '.') {
                    int inicio = ++indice;
                    indice = avancarIdentificador(texto, indice);
                    if (inicio == indice) {
                        throw new IllegalArgumentException("Classe vazia no seletor: " + texto);
                    }
                    classes.add(texto.substring(inicio, indice));
                } else if (caractere == '#') {
                    int inicio = ++indice;
                    indice = avancarIdentificador(texto, indice);
                    if (inicio == indice || id != null) {
                        throw new IllegalArgumentException("Identificador #id invalido no seletor: " + texto);
                    }
                    id = texto.substring(inicio, indice);
                } else if (caractere == '[') {
                    int fim = localizarFimColchete(texto, indice);
                    atributos.add(analisarAtributo(texto.substring(indice + 1, fim)));
                    indice = fim + 1;
                } else {
                    throw new IllegalArgumentException(
                            "Trecho invalido no seletor '" + texto + "' proximo de '" + caractere + "'."
                    );
                }
            }

            if (tag == null && id == null && classes.isEmpty() && atributos.isEmpty()) {
                throw new IllegalArgumentException("Seletor vazio ou invalido: " + texto);
            }
            return new SeletorElemento(tag, id, Set.copyOf(classes), List.copyOf(atributos));
        }

        boolean corresponde(NoHtml no) {
            if (tag != null && !tag.equals("*") && !tag.equals(no.tag())) {
                return false;
            }
            if (id != null && !id.equals(no.atributo("id"))) {
                return false;
            }

            String atributoClasses = no.atributo("class");
            Set<String> classesDoNo = atributoClasses == null
                    ? Set.of()
                    : new HashSet<>(List.of(atributoClasses.trim().split("\\s+")));
            if (!classesDoNo.containsAll(classes)) {
                return false;
            }

            for (RestricaoAtributo atributo : atributos) {
                if (!atributo.corresponde(no)) {
                    return false;
                }
            }
            return true;
        }

        private static RestricaoAtributo analisarAtributo(String conteudo) {
            String texto = conteudo.trim();
            if (texto.isEmpty()) {
                throw new IllegalArgumentException("Atributo vazio no seletor CSS.");
            }

            int igualdade = texto.indexOf('=');
            if (igualdade < 0) {
                return new RestricaoAtributo(texto.toLowerCase(Locale.ROOT), "", false);
            }

            String nome = texto.substring(0, igualdade).trim().toLowerCase(Locale.ROOT);
            String valor = texto.substring(igualdade + 1).trim();
            if (nome.isEmpty() || valor.isEmpty()) {
                throw new IllegalArgumentException("Restricao de atributo invalida: [" + conteudo + "]");
            }
            if ((valor.startsWith("\"") && valor.endsWith("\""))
                    || (valor.startsWith("'") && valor.endsWith("'"))) {
                valor = valor.substring(1, valor.length() - 1);
            }
            return new RestricaoAtributo(nome, valor, true);
        }

        private static int localizarFimColchete(String texto, int inicio) {
            char aspas = 0;
            for (int i = inicio + 1; i < texto.length(); i++) {
                char caractere = texto.charAt(i);
                if (caractere == '\'' || caractere == '"') {
                    if (aspas == 0) {
                        aspas = caractere;
                    } else if (aspas == caractere) {
                        aspas = 0;
                    }
                } else if (caractere == ']' && aspas == 0) {
                    return i;
                }
            }
            throw new IllegalArgumentException("Colchete nao fechado no seletor: " + texto);
        }

        private static int avancarIdentificador(String texto, int indice) {
            while (indice < texto.length()) {
                char c = texto.charAt(indice);
                if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                    indice++;
                } else {
                    break;
                }
            }
            return indice;
        }

        private static boolean eInicioIdentificador(char c) {
            return Character.isLetterOrDigit(c) || c == '-' || c == '_';
        }
    }
}
