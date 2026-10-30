'use strict';

/** Escapa texto falado pelo usuário antes de ir para o SSML. */
function escape(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

module.exports = {
  reprompt: 'Mais alguma coisa?',
  welcomePaired: 'Sintoniza pronto. O que você quer fazer na TV?',
  welcomeNotPaired:
    'Olá! Para começar, abra o app Sintoniza na sua TV. Ele mostra um código de seis dígitos. Qual é o código?',
  askCode: 'Qual é o código de seis dígitos que aparece no app Sintoniza na TV?',
  invalidCodeFormat: 'O código tem seis dígitos. Pode repetir, número por número?',
  codeNotFound: 'Não encontrei esse código. Confira o código na TV e fale de novo, número por número.',
  codeExpired: 'Esse código expirou. Abra o app Sintoniza na TV para gerar um novo e tente de novo.',
  paired: 'Pronto! Sua TV está pareada. Experimente dizer: pede pro Sintoniza abrir o YouTube.',
  unpaired: 'Pronto, esqueci sua TV. Para parear de novo, é só falar o código que aparece no app.',
  wasNotPaired: 'Não tem nenhuma TV pareada com esta Alexa.',
  notPaired:
    'Sua TV ainda não está pareada. Abra o app Sintoniza na TV e me diga o código de seis dígitos que aparece na tela.',
  unreachable:
    'Não consegui falar com a TV. Verifique se ela está ligada e com internet, e abra o app Sintoniza uma vez.',
  whichApp: 'Qual aplicativo você quer abrir?',
  whatToSearch: 'O que você quer procurar no YouTube?',
  whichLevel: 'Para qual volume? Diga um número de zero a cem.',
  whichDirection: 'Não entendi a direção. Diga cima, baixo, esquerda, direita, ok, voltar ou início.',
  ok: 'Ok.',
  opening: (name) => `Abrindo ${escape(name)}.`,
  searching: (query) => `Procurando ${escape(query)} no YouTube.`,
  volumeSet: (level) => `Volume em ${level}.`,
  help:
    'Você pode pedir para abrir um aplicativo, como o YouTube ou a Netflix, pausar, continuar, ' +
    'aumentar ou abaixar o volume, procurar algo no YouTube, ou navegar dizendo cima, baixo, ok e voltar. ' +
    'O que você quer fazer?',
  fallback:
    'Não entendi. Você pode pedir para abrir um aplicativo, pausar, continuar, mudar o volume ou procurar no YouTube. ' +
    'O que você quer fazer?',
  bye: 'Até mais!',
  error: 'Desculpe, algo deu errado. Tente de novo em instantes.',
};
