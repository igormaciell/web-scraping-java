package br.edu.linguagens.esportes;

import br.edu.linguagens.esportes.dominio.FormatoEstatistica;
import br.edu.linguagens.esportes.dominio.ResultadoEstatistica;
import br.edu.linguagens.esportes.dominio.SolicitacaoExtracao;
import br.edu.linguagens.esportes.normalizacao.Normalizadores;
import br.edu.linguagens.esportes.scraping.ExtratorHtml;
import br.edu.linguagens.esportes.scraping.PipelineScraping;

import java.util.function.Function;

/** Suite de testes sem bibliotecas externas, executada por testar.sh ou testar.bat. */
public final class TodosOsTestes {
    private static int aprovados;

    private TodosOsTestes() {
    }

    public static void main(String[] argumentos) throws Exception {
        testar("percentual inteiro", () -> {
            double valor = new Normalizadores().normalizar("Posse: 68%", FormatoEstatistica.PERCENTUAL);
            assertAproximado(0.68, valor);
        });

        testar("percentual com virgula", () -> {
            double valor = new Normalizadores().normalizar("Aproveitamento: 68,5 %", FormatoEstatistica.PERCENTUAL);
            assertAproximado(0.685, valor);
        });

        testar("fracao", () -> {
            double valor = new Normalizadores().normalizar("Arremessos: 15/20", FormatoEstatistica.FRACAO);
            assertAproximado(0.75, valor);
        });

        testar("decimal com virgula", () -> {
            double valor = new Normalizadores().normalizar("Eficiencia: 0,82", FormatoEstatistica.DECIMAL);
            assertAproximado(0.82, valor);
        });

        testar("deteccao automatica", () -> {
            Normalizadores normalizadores = new Normalizadores();
            assertAproximado(0.91, normalizadores.normalizar("Passes: 91%", FormatoEstatistica.AUTOMATICO));
            assertAproximado(0.5, normalizadores.normalizar("Cestas: 5/10", FormatoEstatistica.AUTOMATICO));
            assertAproximado(0.77, normalizadores.normalizar("Indice: 0.77", FormatoEstatistica.AUTOMATICO));
        });

        testar("limites zero e um", () -> {
            Normalizadores normalizadores = new Normalizadores();
            assertAproximado(0.0, normalizadores.normalizar("0%", FormatoEstatistica.PERCENTUAL));
            assertAproximado(1.0, normalizadores.normalizar("20/20", FormatoEstatistica.FRACAO));
        });

        testar("rejeita denominador zero", () -> esperarErro(
                () -> new Normalizadores().normalizar("5/0", FormatoEstatistica.FRACAO),
                "denominador"
        ));

        testar("rejeita valor fora da faixa", () -> esperarErro(
                () -> new Normalizadores().normalizar("120%", FormatoEstatistica.PERCENTUAL),
                "entre 0.0 e 1.0"
        ));

        testar("formato por alias", () -> {
            if (FormatoEstatistica.de("fração") != FormatoEstatistica.FRACAO) {
                throw new AssertionError("O alias 'fração' deveria resultar em FRACAO.");
            }
            if (FormatoEstatistica.de("auto") != FormatoEstatistica.AUTOMATICO) {
                throw new AssertionError("O alias 'auto' deveria resultar em AUTOMATICO.");
            }
        });

        testar("seletor por classe descendente e texto aninhado", () -> {
            String html = "<div class='cartao'><p class='posse'>Posse: <strong>68</strong>%</p></div>";
            String texto = new ExtratorHtml().extrairDeHtml(html, "div.cartao .posse");
            if (!texto.contains("68") || !texto.contains("%")) {
                throw new AssertionError("Texto extraido incorretamente: " + texto);
            }
        });

        testar("seletor por id", () -> {
            String html = "<div><span id='taxa'>Taxa: 91%</span></div>";
            String texto = new ExtratorHtml().extrairDeHtml(html, "#taxa");
            assertContem(texto, "91%");
        });

        testar("seletor por atributo", () -> {
            String html = "<div><span data-estatistica='arremessos'>15/20</span></div>";
            String texto = new ExtratorHtml().extrairDeHtml(
                    html,
                    "[data-estatistica='arremessos']"
            );
            assertContem(texto, "15/20");
        });

        testar("pipeline completo em HTML", () -> {
            String html = "<div class='atleta'><span class='eficiencia'>Eficiencia: 0,82</span></div>";
            SolicitacaoExtracao solicitacao = new SolicitacaoExtracao(
                    "Volei",
                    "Lucas Nunes",
                    "Aproveitamento de saques",
                    "https://exemplo.invalid/volei",
                    "div.atleta .eficiencia",
                    FormatoEstatistica.DECIMAL
            );
            ResultadoEstatistica resultado = new PipelineScraping().executarComHtml(solicitacao, html);
            assertAproximado(0.82, resultado.valorNormalizado());
            assertContem(resultado.valorOriginal(), "0,82");
        });

        testar("funcao customizada como valor de primeira classe", () -> {
            Function<String, Double> notaDeDez = texto -> {
                String numero = texto.replaceAll("[^0-9.,-]", "").replace(',', '.');
                return Double.parseDouble(numero) / 10.0;
            };
            double valor = new PipelineScraping().normalizarComFuncaoCustomizada("Nota: 8", notaDeDez);
            assertAproximado(0.8, valor);
        });

        testar("seletor inexistente gera erro", () -> esperarErro(
                () -> new ExtratorHtml().extrairDeHtml("<div>68%</div>", ".nao-existe"),
                "Nenhum elemento"
        ));

        testar("combinador CSS nao suportado gera mensagem clara", () -> esperarErro(
                () -> new ExtratorHtml().extrairDeHtml("<div><span>68%</span></div>", "div > span"),
                "nao suportado"
        ));

        System.out.println("------------------------------------------------------------");
        System.out.println("TOTAL: " + aprovados + " testes aprovados.");
        System.out.println("Todos os cenarios foram executados com sucesso.");
    }

    private static void testar(String nome, AcaoTeste acao) throws Exception {
        try {
            acao.executar();
            aprovados++;
            System.out.println("[OK] " + nome);
        } catch (Throwable erro) {
            System.err.println("[FALHOU] " + nome + ": " + erro.getMessage());
            if (erro instanceof Exception excecao) {
                throw excecao;
            }
            throw erro;
        }
    }

    private static void assertAproximado(double esperado, double atual) {
        if (Math.abs(esperado - atual) > 1.0e-9) {
            throw new AssertionError("Esperado " + esperado + ", obtido " + atual + ".");
        }
    }

    private static void assertContem(String texto, String trecho) {
        if (texto == null || !texto.contains(trecho)) {
            throw new AssertionError("O texto '" + texto + "' nao contem '" + trecho + "'.");
        }
    }

    private static void esperarErro(AcaoTeste acao, String trechoMensagem) throws Exception {
        try {
            acao.executar();
            throw new AssertionError("Era esperada uma excecao contendo: " + trechoMensagem);
        } catch (IllegalArgumentException erro) {
            assertContem(erro.getMessage(), trechoMensagem);
        }
    }

    @FunctionalInterface
    private interface AcaoTeste {
        void executar() throws Exception;
    }
}
