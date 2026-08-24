package br.edu.linguagens.esportes.dominio;

import java.time.Instant;

/**
 * Saida padronizada do pipeline. O valor normalizado sempre pertence ao intervalo [0.0, 1.0].
 */
public record ResultadoEstatistica(
        String esporte,
        String atleta,
        String metrica,
        String valorOriginal,
        double valorNormalizado,
        FormatoEstatistica formato,
        String fonte,
        Instant coletadoEm
) {
}
