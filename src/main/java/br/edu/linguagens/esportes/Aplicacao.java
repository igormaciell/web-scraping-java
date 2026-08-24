package br.edu.linguagens.esportes;

import br.edu.linguagens.esportes.web.ServidorWeb;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/** Ponto de entrada da aplicacao. */
public final class Aplicacao {
    private static final int PORTA_PADRAO = 8080;

    private Aplicacao() {
    }

    public static void main(String[] argumentos) throws Exception {
        int porta = descobrirPorta(argumentos);
        ServidorWeb servidor = new ServidorWeb(porta);
        servidor.iniciar();

        Runtime.getRuntime().addShutdownHook(new Thread(servidor::parar, "encerramento-servidor"));

        System.out.println("============================================================");
        System.out.println(" Extrator de Estatisticas Esportivas iniciado com sucesso");
        System.out.println(" Interface: http://localhost:" + porta + "/");
        System.out.println(" Saude:     http://localhost:" + porta + "/api/saude");
        System.out.println(" Use Ctrl+C para encerrar.");
        System.out.println("============================================================");

        new CountDownLatch(1).await();
    }

    private static int descobrirPorta(String[] argumentos) throws IOException {
        String portaTexto = System.getenv("PORT");
        for (String argumento : argumentos) {
            if (argumento.startsWith("--porta=")) {
                portaTexto = argumento.substring("--porta=".length());
            } else if (argumento.startsWith("--port=")) {
                portaTexto = argumento.substring("--port=".length());
            }
        }

        if (portaTexto == null || portaTexto.isBlank()) {
            return PORTA_PADRAO;
        }

        try {
            int porta = Integer.parseInt(portaTexto.trim());
            if (porta < 1 || porta > 65_535) {
                throw new NumberFormatException("fora da faixa");
            }
            return porta;
        } catch (NumberFormatException erro) {
            throw new IOException("Porta invalida: '" + portaTexto + "'.", erro);
        }
    }
}
