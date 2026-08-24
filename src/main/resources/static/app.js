const elementos = {
    formularioScraping: document.querySelector('#formulario-scraping'),
    formularioNormalizacao: document.querySelector('#formulario-normalizacao'),
    botaoDemonstracao: document.querySelector('#botao-demonstracao'),
    botaoRaspar: document.querySelector('#botao-raspar'),
    corpoResultados: document.querySelector('#corpo-resultados'),
    contadorResultados: document.querySelector('#contador-resultados'),
    mensagem: document.querySelector('#mensagem'),
    saidaNormalizacao: document.querySelector('#saida-normalizacao'),
    indicadorSaude: document.querySelector('#indicador-saude')
};

const exemplos = {
    futebol: {
        url: `${location.origin}/examples/futebol.html`,
        seletor: 'div.cartao-jogador .posse-bola',
        formato: 'PERCENTUAL',
        esporte: 'Futebol',
        atleta: 'Equipe Aurora',
        metrica: 'Posse de bola'
    },
    basquete: {
        url: `${location.origin}/examples/basquete.html`,
        seletor: "[data-estatistica='arremessos']",
        formato: 'FRACAO',
        esporte: 'Basquete',
        atleta: 'Marina Costa',
        metrica: 'Eficiencia de arremessos'
    },
    volei: {
        url: `${location.origin}/examples/volei.html`,
        seletor: 'div.atleta .eficiencia',
        formato: 'DECIMAL',
        esporte: 'Volei',
        atleta: 'Lucas Nunes',
        metrica: 'Aproveitamento de saques'
    }
};

function preencherFormulario(exemplo) {
    Object.entries(exemplo).forEach(([nome, valor]) => {
        const campo = elementos.formularioScraping.elements.namedItem(nome);
        if (campo) campo.value = valor;
    });
}

async function buscarJson(url) {
    const resposta = await fetch(url, {headers: {'Accept': 'application/json'}});
    let dados;
    try {
        dados = await resposta.json();
    } catch {
        throw new Error(`Resposta invalida do servidor (HTTP ${resposta.status}).`);
    }
    if (!resposta.ok) {
        throw new Error(dados.mensagem || `Falha HTTP ${resposta.status}.`);
    }
    return dados;
}

function mostrarMensagem(texto) {
    elementos.mensagem.textContent = texto;
    elementos.mensagem.classList.remove('oculto');
}

function limparMensagem() {
    elementos.mensagem.textContent = '';
    elementos.mensagem.classList.add('oculto');
}

function criarCelula(texto) {
    const celula = document.createElement('td');
    celula.textContent = texto;
    return celula;
}

function renderizarResultados(resultados) {
    elementos.corpoResultados.replaceChildren();
    elementos.contadorResultados.textContent = `${resultados.length} ${resultados.length === 1 ? 'registro' : 'registros'}`;

    if (!resultados.length) {
        const linha = document.createElement('tr');
        linha.className = 'vazio';
        const celula = criarCelula('Nenhum resultado retornado.');
        celula.colSpan = 7;
        linha.append(celula);
        elementos.corpoResultados.append(linha);
        return;
    }

    for (const resultado of resultados) {
        const linha = document.createElement('tr');
        linha.append(criarCelula(resultado.esporte));
        linha.append(criarCelula(resultado.atleta));
        linha.append(criarCelula(resultado.metrica));
        linha.append(criarCelula(resultado.valorOriginal));

        const normalizado = document.createElement('td');
        const destaque = document.createElement('strong');
        destaque.textContent = Number(resultado.valorNormalizado).toLocaleString('pt-BR', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 4
        });
        normalizado.append(destaque);
        linha.append(normalizado);

        const formato = document.createElement('td');
        const selo = document.createElement('span');
        selo.className = 'selo-formato';
        selo.textContent = resultado.formato;
        formato.append(selo);
        linha.append(formato);

        const coleta = document.createElement('td');
        const data = document.createElement('div');
        data.textContent = new Date(resultado.coletadoEm).toLocaleString('pt-BR');
        coleta.append(data);
        const fonte = document.createElement('a');
        fonte.href = resultado.fonte;
        fonte.target = '_blank';
        fonte.rel = 'noopener noreferrer';
        fonte.textContent = 'abrir fonte';
        coleta.append(fonte);
        linha.append(coleta);

        elementos.corpoResultados.append(linha);
    }
}

async function executarScraping(evento) {
    evento.preventDefault();
    limparMensagem();
    elementos.botaoRaspar.disabled = true;
    elementos.botaoRaspar.textContent = 'Coletando...';

    try {
        const dadosFormulario = new FormData(elementos.formularioScraping);
        const parametros = new URLSearchParams(dadosFormulario);
        const resultado = await buscarJson(`/api/raspar?${parametros}`);
        renderizarResultados([resultado]);
    } catch (erro) {
        mostrarMensagem(erro.message);
    } finally {
        elementos.botaoRaspar.disabled = false;
        elementos.botaoRaspar.textContent = 'Raspar e normalizar';
    }
}

async function executarDemonstracao() {
    limparMensagem();
    elementos.botaoDemonstracao.disabled = true;
    elementos.botaoDemonstracao.textContent = 'Executando...';
    try {
        const dados = await buscarJson('/api/demonstracao');
        renderizarResultados(dados.resultados || []);
    } catch (erro) {
        mostrarMensagem(erro.message);
    } finally {
        elementos.botaoDemonstracao.disabled = false;
        elementos.botaoDemonstracao.textContent = 'Executar demonstracao completa';
    }
}

async function executarNormalizacao(evento) {
    evento.preventDefault();
    const dadosFormulario = new FormData(elementos.formularioNormalizacao);
    const parametros = new URLSearchParams(dadosFormulario);
    elementos.saidaNormalizacao.textContent = '...';
    try {
        const dados = await buscarJson(`/api/normalizar?${parametros}`);
        elementos.saidaNormalizacao.textContent = Number(dados.valorNormalizado).toLocaleString('pt-BR', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 4
        });
    } catch (erro) {
        elementos.saidaNormalizacao.textContent = 'erro';
        mostrarMensagem(erro.message);
    }
}

async function verificarSaude() {
    try {
        await buscarJson('/api/saude');
        elementos.indicadorSaude.classList.add('ativo');
        elementos.indicadorSaude.lastChild.textContent = ' servidor ativo';
    } catch {
        elementos.indicadorSaude.classList.remove('ativo');
        elementos.indicadorSaude.lastChild.textContent = ' servidor indisponivel';
    }
}

document.querySelectorAll('[data-exemplo]').forEach(botao => {
    botao.addEventListener('click', () => preencherFormulario(exemplos[botao.dataset.exemplo]));
});

elementos.formularioScraping.addEventListener('submit', executarScraping);
elementos.formularioNormalizacao.addEventListener('submit', executarNormalizacao);
elementos.botaoDemonstracao.addEventListener('click', executarDemonstracao);

preencherFormulario(exemplos.futebol);
verificarSaude();
