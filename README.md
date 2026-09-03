# Extrator de Estatisticas Esportivas em Tempo Real

Projeto academico da disciplina **Linguagens de Programacao** do 6° período da minha graduação em Ciencia da computacao na Universidade Vila Velha - UVV.

Desenvolvido em Java Web para coletar dados presentes em HTML e transformar estatisticas heterogeneas em valores decimais no intervalo **0.0 a 1.0**.

## O que foi implementado

- servidor web e API REST usando recursos nativos do Java;
- cliente HTTP que busca o HTML no momento da requisicao;
- parser HTML e seletor CSS didatico, sem dependencia externa;
- normalizadores para percentual, fracao, decimal e deteccao automatica;
- uso explicito de identificadores, funcoes de primeira ordem e funcoes de primeira classe;
- interface web responsiva para demonstrar a solucao;
- paginas HTML locais de futebol, basquete e volei;
- tratamento de erros de entrada, HTTP, seletor e valores fora da faixa;
- 16 testes automatizados executaveis sem bibliotecas de teste externas;
- JAR executavel pronto em `dist/extrator-estatisticas-esportivas.jar`.

## Requisitos

- JDK 17 ou superior;
- navegador moderno para acessar a interface.

Nao e necessario instalar servidor de aplicacao, banco de dados ou bibliotecas adicionais.

## Executar rapidamente

### Linux ou macOS

```bash
./executar.sh
```

### Windows

```bat
executar.bat
```

Depois, acesse:

```text
http://localhost:8080
```

Para usar outra porta:

```bash
./executar.sh --porta=9090
```

Tambem e possivel executar diretamente o artefato pronto:

```bash
java -jar dist/extrator-estatisticas-esportivas.jar
```

## Compilar e testar

### Linux ou macOS

```bash
./testar.sh
```

### Windows

```bat
testar.bat
```

Resultado esperado:

```text
TOTAL: 16 testes aprovados.
Todos os cenarios foram executados com sucesso.
```

A compilacao isolada pode ser feita com `./compilar.sh` ou `compilar.bat`.

### Maven, opcional

O projeto tambem possui `pom.xml`:

```bash
mvn clean package
java -jar target/extrator-estatisticas-esportivas.jar
```

Os scripts incluidos sao a forma recomendada, pois funcionam apenas com o JDK e nao dependem de download de plugins.

## Endpoints da API

### Verificar o servidor

```http
GET /api/saude
```

### Normalizar um valor sem scraping

```http
GET /api/normalizar?valor=Posse%3A%2068%25&formato=PERCENTUAL
```

Resposta:

```json
{
  "valorOriginal": "Posse: 68%",
  "formato": "PERCENTUAL",
  "valorNormalizado": 0.68
}
```

### Raspar uma pagina e normalizar

```http
GET /api/raspar?url=URL&seletor=SELETOR&formato=FORMATO&esporte=ESPORTE&atleta=ATLETA&metrica=METRICA
```

Parametros obrigatorios:

| Parametro | Descricao |
|---|---|
| `url` | Endereco HTTP ou HTTPS do portal |
| `seletor` | Seletor usado para localizar a metrica |
| `formato` | `PERCENTUAL`, `FRACAO`, `DECIMAL` ou `AUTOMATICO` |

Os parametros `esporte`, `atleta` e `metrica` possuem valores padrao, mas devem ser informados para gerar uma resposta descritiva.

Exemplo local:

```bash
curl --get "http://localhost:8080/api/raspar" \
  --data-urlencode "url=http://localhost:8080/examples/futebol.html" \
  --data-urlencode "seletor=div.cartao-jogador .posse-bola" \
  --data-urlencode "formato=PERCENTUAL" \
  --data-urlencode "esporte=Futebol" \
  --data-urlencode "atleta=Equipe Aurora" \
  --data-urlencode "metrica=Posse de bola"
```

### Demonstracao com diferentes esportes

```http
GET /api/demonstracao
```

Esse endpoint processa quatro metricas:

| Esporte | Valor original | Normalizado |
|---|---:|---:|
| Futebol | `68%` | `0.68` |
| Futebol | `91%` | `0.91` |
| Basquete | `15/20` | `0.75` |
| Volei | `0,82` | `0.82` |

## Formulas de normalizacao

### Percentual

```text
valorNormalizado = percentual / 100
68% = 68 / 100 = 0.68
```

### Fracao

```text
valorNormalizado = numerador / denominador
15/20 = 15 / 20 = 0.75
```

### Decimal

O numero e interpretado diretamente, desde que ja esteja entre 0.0 e 1.0:

```text
0,82 = 0.82
```

Valores menores que 0.0, maiores que 1.0, denominadores iguais a zero e textos sem numero valido geram resposta de erro.

## Seletores aceitos

O parser implementa um subconjunto didatico e suficiente para a atividade:

- tag: `span`;
- classe: `.posse-bola`;
- identificador: `#taxa-passes`;
- tag e classe: `div.atleta`;
- atributo: `[data-estatistica]`;
- atributo e valor: `[data-estatistica='arremessos']`;
- descendente: `div.cartao-jogador .posse-bola`.

Combinadores como `>`, `+`, `~`, pseudoclasses e seletores separados por virgula nao fazem parte do escopo e retornam uma mensagem clara.

## Onde os conceitos aparecem

### Identificadores

Classes, metodos, variaveis e constantes possuem nomes semanticos, por exemplo:

- `PipelineScraping`;
- `SolicitacaoExtracao`;
- `valorNormalizado`;
- `LIMITE_MAXIMO_HTML_BYTES`;
- `FormatoEstatistica.PERCENTUAL`.

### Funcoes de primeira ordem

Metodos como `normalizarPercentual`, `normalizarFracao`, `limparTexto` e `validarFaixaUnitaria` recebem valores comuns e retornam valores comuns. Eles nao recebem nem devolvem funcoes.

### Funcoes de primeira classe

A classe `Normalizadores` armazena comportamentos em:

```java
Map<FormatoEstatistica, Function<String, Double>>
```

Referencias como `Normalizadores::normalizarPercentual` sao tratadas como valores. O metodo `aplicar` recebe uma `Function<String, Double>`, e o pipeline escolhe e passa a estrategia adequada em tempo de execucao.

## Estrutura principal

```text
src/main/java/br/edu/linguagens/esportes/
├── Aplicacao.java
├── dominio/
│   ├── FormatoEstatistica.java
│   ├── ResultadoEstatistica.java
│   └── SolicitacaoExtracao.java
├── normalizacao/
│   └── Normalizadores.java
├── scraping/
│   ├── AnalisadorHtml.java
│   ├── ClienteHttpHtml.java
│   ├── ExtratorHtml.java
│   ├── NoHtml.java
│   ├── PipelineScraping.java
│   └── SeletorCssSimples.java
└── web/
    ├── HttpUtil.java
    ├── JsonUtil.java
    └── ServidorWeb.java
```

## Observacoes tecnicas e eticas

- Cada chamada a `/api/raspar` realiza uma nova requisicao HTTP; a resposta nao usa cache de estatisticas.
- O extrator trabalha com HTML recebido pelo servidor. Conteudo criado apenas por JavaScript no navegador exige uma ferramenta de automacao de navegador e esta fora do escopo.
- Em portais reais, o seletor deve ser adaptado ao HTML de cada pagina.
- A coleta deve respeitar autorizacao, termos de uso, `robots.txt`, limites de acesso e legislacao aplicavel.

## Entrega

1. Preencha nome, turma, professor e instituicao em `RELATORIO.md`.
2. Execute `testar.sh` ou `testar.bat`.
3. Envie o arquivo ZIP do projeto, mantendo o codigo-fonte e o relatorio.
