package br.edu.linguagens.esportes.normalizacao;

import br.edu.linguagens.esportes.dominio.FormatoEstatistica;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registro das estrategias de normalizacao.
 *
 * <p>FUNCOES DE PRIMEIRA CLASSE: referencias de metodo sao armazenadas em um mapa,
 * retornadas por {@link #obter(FormatoEstatistica)} e recebidas como parametro por
 * {@link #aplicar(String, Function)}.</p>
 *
 * <p>FUNCOES DE PRIMEIRA ORDEM: os metodos normalizarPercentual,
 * normalizarFracao, normalizarDecimal, limparTexto e validarFaixaUnitaria recebem
 * apenas dados e devolvem dados; eles nao recebem nem retornam outras funcoes.</p>
 */
public final class Normalizadores {
    private static final Pattern PADRAO_PERCENTUAL = Pattern.compile(
            "([-+]?\\d+(?:[.,]\\d+)?)\\s*%"
    );
    private static final Pattern PADRAO_FRACAO = Pattern.compile(
            "([-+]?\\d+(?:[.,]\\d+)?)\\s*/\\s*([-+]?\\d+(?:[.,]\\d+)?)"
    );
    private static final Pattern PADRAO_NUMERO = Pattern.compile(
            "[-+]?\\d+(?:[.,]\\d+)?"
    );
    private static final double TOLERANCIA = 1.0e-12;

    private final Map<FormatoEstatistica, Function<String, Double>> funcoesPorFormato;

    public Normalizadores() {
        EnumMap<FormatoEstatistica, Function<String, Double>> registro =
                new EnumMap<>(FormatoEstatistica.class);

        // As referencias de metodo abaixo sao valores: podem ser armazenadas e passadas adiante.
        registro.put(FormatoEstatistica.PERCENTUAL, Normalizadores::normalizarPercentual);
        registro.put(FormatoEstatistica.FRACAO, Normalizadores::normalizarFracao);
        registro.put(FormatoEstatistica.DECIMAL, Normalizadores::normalizarDecimal);
        registro.put(FormatoEstatistica.AUTOMATICO, Normalizadores::normalizarAutomaticamente);

        this.funcoesPorFormato = Map.copyOf(registro);
    }

    /** Retorna uma funcao como valor de primeira classe. */
    public Function<String, Double> obter(FormatoEstatistica formato) {
        Objects.requireNonNull(formato, "formato");
        Function<String, Double> normalizador = funcoesPorFormato.get(formato);
        if (normalizador == null) {
            throw new IllegalArgumentException("Nao existe normalizador para " + formato + ".");
        }
        return normalizador;
    }

    public double normalizar(String valorOriginal, FormatoEstatistica formato) {
        return aplicar(valorOriginal, obter(formato));
    }

    /**
     * Funcao de ordem superior: recebe outra funcao como argumento.
     * Isso permite incluir novos formatos sem alterar o restante do pipeline.
     */
    public double aplicar(String valorOriginal, Function<String, Double> normalizador) {
        Objects.requireNonNull(normalizador, "normalizador");
        String textoLimpo = limparTexto(valorOriginal);
        double valorCalculado = normalizador.apply(textoLimpo);
        return validarFaixaUnitaria(valorCalculado);
    }

    public static String limparTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("O valor original da estatistica esta vazio.");
        }
        return texto.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    public static double validarFaixaUnitaria(double valor) {
        if (!Double.isFinite(valor)) {
            throw new IllegalArgumentException("O valor calculado nao e um numero finito.");
        }
        if (valor < -TOLERANCIA || valor > 1.0 + TOLERANCIA) {
            throw new IllegalArgumentException(
                    "A estatistica normalizada deve estar entre 0.0 e 1.0, mas resultou em " + valor + "."
            );
        }
        if (Math.abs(valor) <= TOLERANCIA) {
            return 0.0;
        }
        if (Math.abs(valor - 1.0) <= TOLERANCIA) {
            return 1.0;
        }
        return valor;
    }

    private static double normalizarPercentual(String texto) {
        Matcher matcher = PADRAO_PERCENTUAL.matcher(texto);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Percentual nao encontrado. Exemplo aceito: 'Posse: 68%'."
            );
        }
        return converterNumero(matcher.group(1)) / 100.0;
    }

    private static double normalizarFracao(String texto) {
        Matcher matcher = PADRAO_FRACAO.matcher(texto);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Fracao nao encontrada. Exemplo aceito: 'Arremessos: 15/20'."
            );
        }

        double numerador = converterNumero(matcher.group(1));
        double denominador = converterNumero(matcher.group(2));
        if (denominador == 0.0) {
            throw new IllegalArgumentException("O denominador da fracao nao pode ser zero.");
        }
        return numerador / denominador;
    }

    private static double normalizarDecimal(String texto) {
        Matcher matcher = PADRAO_NUMERO.matcher(texto);
        String ultimoNumero = null;
        while (matcher.find()) {
            ultimoNumero = matcher.group();
        }
        if (ultimoNumero == null) {
            throw new IllegalArgumentException(
                    "Numero decimal nao encontrado. Exemplo aceito: 'Eficiencia: 0,82'."
            );
        }
        return converterNumero(ultimoNumero);
    }

    private static double normalizarAutomaticamente(String texto) {
        if (PADRAO_PERCENTUAL.matcher(texto).find()) {
            return normalizarPercentual(texto);
        }
        if (PADRAO_FRACAO.matcher(texto).find()) {
            return normalizarFracao(texto);
        }
        return normalizarDecimal(texto);
    }

    private static double converterNumero(String textoNumerico) {
        try {
            return Double.parseDouble(textoNumerico.replace(',', '.'));
        } catch (NumberFormatException erro) {
            throw new IllegalArgumentException(
                    "Numero invalido na estatistica: '" + textoNumerico + "'.",
                    erro
            );
        }
    }
}
