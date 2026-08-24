# Relatorio da Atividade

## Extrator de Estatisticas Esportivas em Tempo Real

**Disciplina:** Linguagens de Programacao  
**Aluno(a):** [PREENCHER]  
**Matricula:** [PREENCHER]  
**Turma:** [PREENCHER]  
**Professor(a):** [PREENCHER]  
**Instituicao:** [PREENCHER]  
**Data:** [PREENCHER]

---

## 1. Introducao

Portais esportivos exibem estatisticas utilizando formatos diferentes. Uma taxa de posse de bola pode aparecer como `68%`, enquanto o aproveitamento de arremessos pode ser exibido como `15/20`. Essa heterogeneidade dificulta a comparacao direta e o armazenamento uniforme dos dados.

O objetivo deste trabalho foi desenvolver uma aplicacao Java Web capaz de:

1. buscar o HTML de um portal esportivo;
2. localizar uma estatistica por seletor;
3. interpretar seu formato original;
4. normalizar o valor para o intervalo de `0.0` a `1.0`;
5. devolver uma resposta estruturada pela API;
6. demonstrar os conceitos de identificadores, funcoes de primeira ordem e funcoes de primeira classe.

A solucao foi implementada somente com recursos do JDK 17. Isso reduz dependencias e torna o projeto executavel em qualquer ambiente com Java compativel.

## 2. Requisitos atendidos

| Requisito | Implementacao |
|---|---|
| Java Web | Servidor HTTP com rotas de API e interface no navegador |
| Web scraping | Cliente HTTP, parser HTML e selecao de elemento |
| Diferentes esportes | Exemplos de futebol, basquete e volei |
| Percentual | `68%` transformado em `0.68` |
| Fracao | `15/20` transformado em `0.75` |
| Decimal | `0,82` transformado em `0.82` |
| Padronizacao | Validacao obrigatoria no intervalo `[0.0, 1.0]` |
| Identificadores | Nomes descritivos em todos os componentes |
| Funcoes de 1ª ordem | Metodos que recebem e retornam dados comuns |
| Funcoes de 1ª classe | Estrategias armazenadas e passadas como `Function` |
| Evidencia de qualidade | 16 testes automatizados aprovados |

## 3. Arquitetura da solucao

O sistema foi separado em quatro camadas principais:

```text
Navegador / Cliente HTTP
          |
          v
ServidorWeb e endpoints REST
          |
          v
PipelineScraping
   |             |
   v             v
ExtratorHtml   Normalizadores
   |             |
   v             v
HTTP + parser   Function<String, Double>
          |
          v
ResultadoEstatistica (JSON)
```

### 3.1 Camada de dominio

A camada de dominio representa os dados usados no processamento:

- `FormatoEstatistica`: enumera percentual, fracao, decimal e automatico;
- `SolicitacaoExtracao`: agrupa esporte, atleta, metrica, URL, seletor e formato;
- `ResultadoEstatistica`: representa a resposta padronizada e inclui o instante da coleta.

### 3.2 Camada de scraping

A classe `ClienteHttpHtml` usa `HttpClient` para realizar uma requisicao HTTP no momento em que o endpoint e chamado. O HTML recebido e processado por `AnalisadorHtml`, que cria uma arvore simples de elementos.

A classe `SeletorCssSimples` localiza o primeiro elemento correspondente. Sao aceitos seletores por tag, identificador, classe, atributo e relacao de descendencia. Em seguida, `ExtratorHtml` recupera o texto visivel do elemento.

### 3.3 Camada de normalizacao

A classe `Normalizadores` contem uma estrategia para cada formato. Todas as estrategias produzem um `double`, e o resultado e validado para garantir que esteja entre `0.0` e `1.0`.

### 3.4 Camada web

A classe `ServidorWeb` disponibiliza:

- `/`: interface grafica;
- `/api/saude`: verificacao do servidor;
- `/api/normalizar`: normalizacao sem acesso externo;
- `/api/raspar`: scraping e normalizacao;
- `/api/demonstracao`: execucao de exemplos dos tres esportes.

## 4. Algoritmos de normalizacao

### 4.1 Percentual

Para uma estatistica percentual, o numero e dividido por 100:

```text
p = 68
normalizado = p / 100
normalizado = 68 / 100
normalizado = 0.68
```

A expressao regular identifica o numero imediatamente associado ao simbolo `%`, aceitando ponto ou virgula decimal.

### 4.2 Fracao

Para uma fracao, o numerador e dividido pelo denominador:

```text
numerador = 15
denominador = 20
normalizado = numerador / denominador
normalizado = 15 / 20
normalizado = 0.75
```

O sistema rejeita denominador igual a zero. Uma fracao que resulte em valor menor que `0.0` ou maior que `1.0` tambem e rejeitada.

### 4.3 Decimal

Um valor decimal ja padronizado e convertido para `double`:

```text
"0,82" -> 0.82
```

O separador decimal brasileiro e convertido de virgula para ponto antes da interpretacao numerica.

### 4.4 Formato automatico

No modo `AUTOMATICO`, a ordem de deteccao e:

1. percentual, quando existe `%`;
2. fracao, quando existe `numero/numero`;
3. decimal, nos demais casos numericos.

## 5. Aplicacao dos conceitos da disciplina

### 5.1 Identificadores

Identificadores sao nomes associados a elementos do programa. Foram utilizados nomes claros para comunicar a responsabilidade de cada componente.

Exemplos:

```java
private static final int LIMITE_MAXIMO_HTML_BYTES = 2_000_000;

double valorNormalizado;

public ResultadoEstatistica executar(SolicitacaoExtracao solicitacao)
```

Os nomes permitem compreender a finalidade do codigo sem depender de comentarios excessivos. As classes seguem `PascalCase`, enquanto metodos e variaveis seguem `camelCase`. Constantes usam letras maiusculas e sublinhado.

### 5.2 Funcoes de primeira ordem

Uma funcao de primeira ordem trabalha apenas com valores comuns: ela nao recebe outra funcao como argumento e nao retorna uma funcao.

Exemplos presentes em `Normalizadores`:

```java
private static double normalizarPercentual(String texto)
private static double normalizarFracao(String texto)
private static double normalizarDecimal(String texto)
public static String limparTexto(String texto)
public static double validarFaixaUnitaria(double valor)
```

Esses metodos recebem `String` ou `double` e retornam `double` ou `String`.

### 5.3 Funcoes de primeira classe

Em Java, funcoes nao existem como valores independentes da mesma forma que em algumas linguagens funcionais. Entretanto, lambdas e referencias de metodo podem ser representadas por interfaces funcionais, como `Function<T, R>`.

O projeto usa o seguinte registro:

```java
Map<FormatoEstatistica, Function<String, Double>> funcoesPorFormato;
```

As estrategias sao armazenadas como valores:

```java
registro.put(
    FormatoEstatistica.PERCENTUAL,
    Normalizadores::normalizarPercentual
);

registro.put(
    FormatoEstatistica.FRACAO,
    Normalizadores::normalizarFracao
);
```

O metodo `obter` retorna uma funcao:

```java
public Function<String, Double> obter(FormatoEstatistica formato)
```

O metodo `aplicar` recebe uma funcao:

```java
public double aplicar(
    String valorOriginal,
    Function<String, Double> normalizador
)
```

Portanto, o comportamento de normalizacao pode ser armazenado, selecionado, retornado e passado como argumento. Esse uso caracteriza as funcoes como valores de primeira classe no contexto das interfaces funcionais de Java.

### 5.4 Funcao de ordem superior

Embora o enunciado destaque funcoes de primeira classe e de primeira ordem, o uso de funcoes como argumentos naturalmente cria funcoes de ordem superior. O metodo `aplicar`, por exemplo, recebe outra funcao e a executa.

Essa separacao permite adicionar um novo formato sem reescrever o pipeline de scraping. Basta registrar outra `Function<String, Double>`.

## 6. Fluxo de execucao

Ao chamar `/api/raspar`, o sistema executa as seguintes etapas:

1. valida os parametros recebidos;
2. cria uma `SolicitacaoExtracao`;
3. realiza a requisicao HTTP para a URL informada;
4. verifica o codigo de status e o tamanho da resposta;
5. analisa o HTML;
6. encontra o elemento pelo seletor;
7. extrai seu texto;
8. escolhe a funcao de normalizacao;
9. aplica a funcao ao texto;
10. valida a faixa `[0.0, 1.0]`;
11. cria um `ResultadoEstatistica`;
12. serializa o resultado em JSON.

## 7. Exemplo de entrada e saida

### Entrada

```text
URL: http://localhost:8080/examples/basquete.html
Seletor: [data-estatistica='arremessos']
Formato: FRACAO
Esporte: Basquete
Atleta: Marina Costa
Metrica: Eficiencia de arremessos
```

### Texto encontrado

```text
Arremessos convertidos: 15/20
```

### Saida

```json
{
  "esporte": "Basquete",
  "atleta": "Marina Costa",
  "metrica": "Eficiencia de arremessos",
  "valorOriginal": "Arremessos convertidos: 15/20",
  "valorNormalizado": 0.75,
  "formato": "FRACAO",
  "fonte": "http://localhost:8080/examples/basquete.html",
  "coletadoEm": "instante ISO-8601"
}
```

## 8. Tratamento de erros

Foram tratados os seguintes casos:

- parametro obrigatorio ausente;
- formato desconhecido;
- URL invalida ou protocolo diferente de HTTP/HTTPS;
- falha de conexao ou tempo limite;
- resposta HTTP diferente de sucesso;
- HTML acima do limite definido;
- seletor invalido ou nao encontrado;
- elemento sem texto;
- percentual sem `%`;
- fracao sem `/`;
- denominador igual a zero;
- decimal ausente;
- resultado fora do intervalo permitido.

A API usa codigos HTTP adequados:

- `200`: sucesso;
- `400`: dados de entrada invalidos;
- `405`: metodo HTTP nao permitido;
- `502`: falha ao acessar o portal;
- `500`: falha interna inesperada.

## 9. Testes realizados

A suite `TodosOsTestes` executa 16 cenarios:

1. percentual inteiro;
2. percentual com virgula;
3. fracao;
4. decimal com virgula;
5. deteccao automatica;
6. limites zero e um;
7. rejeicao de denominador zero;
8. rejeicao de valor fora da faixa;
9. aliases de formato;
10. seletor por classe e descendente;
11. seletor por identificador;
12. seletor por atributo;
13. pipeline completo com HTML;
14. funcao customizada de primeira classe;
15. seletor inexistente;
16. combinador CSS fora do escopo.

Resultado obtido:

```text
TOTAL: 16 testes aprovados.
Todos os cenarios foram executados com sucesso.
```

Tambem foi realizado um teste de ponta a ponta com o servidor em execucao. Os endpoints `/api/saude`, `/api/demonstracao` e `/api/raspar` responderam corretamente.

## 10. Limitacoes e possiveis evolucoes

A versao atual possui um seletor CSS propositalmente didatico. Uma evolucao poderia integrar uma biblioteca completa de seletores. O sistema tambem processa o HTML retornado pelo servidor; paginas cujo conteudo aparece somente depois da execucao de JavaScript exigiriam automacao de navegador.

Outras evolucoes possiveis:

- extracao de varias metricas em uma unica chamada;
- perfis configuraveis por portal;
- armazenamento historico em banco de dados;
- autenticacao da API;
- controle de taxa por dominio;
- cache com tempo de expiracao;
- painel com graficos de evolucao;
- agendamento de coletas.

Em uso real, a coleta deve respeitar termos de uso, `robots.txt`, limites de requisicao e a legislacao aplicavel.

## 11. Conclusao

O trabalho implementou um pipeline completo de Java Web e web scraping capaz de unificar estatisticas esportivas com representacoes diferentes. Percentuais, fracoes e decimais sao convertidos para uma escala comum entre `0.0` e `1.0`.

A arquitetura separa dominio, coleta, parsing, normalizacao e apresentacao. A aplicacao dos conceitos da disciplina e observavel no codigo: identificadores comunicam significado, funcoes de primeira ordem realizam transformacoes diretas e funcoes representadas por `Function<String, Double>` sao manipuladas como valores de primeira classe.

Os testes confirmam os resultados esperados e os principais cenarios de erro. Dessa forma, a solucao atende ao problema proposto e permanece simples de executar, analisar e expandir.

## 12. Referencias

- Oracle. Java SE 17 API — `HttpClient`.
  https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpClient.html
- Oracle. Java SE 17 API — `HttpServer`.
  https://docs.oracle.com/en/java/javase/17/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html
- Oracle. Java SE 17 API — `Function`.
  https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/function/Function.html
- Apache Maven Project. Maven Compiler Plugin.
  https://maven.apache.org/plugins/maven-compiler-plugin/
