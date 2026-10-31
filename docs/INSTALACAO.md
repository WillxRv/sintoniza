# Instalação (usuário)

Você precisa de: uma Android TV / Google TV (Mi TV Stick, Chromecast, TV box…), um Echo ou o app Alexa, e os dois conectados à internet.

## 1. Instale o app na TV

Enquanto o app não estiver na Play Store:

1. Na TV, instale o app **Downloader** (pela Play Store).
2. Em **Configurações → Apps → Segurança e restrições → Fontes desconhecidas**, permita o Downloader.
3. Abra o Downloader e digite o endereço abaixo, que sempre aponta para a versão mais recente:
   ```
   github.com/WillxRv/sintoniza/releases/latest/download/sintoniza.apk
   ```
4. Instale.

## 2. Configure o app

Abra o **Sintoniza** na TV.

- **Abrir apps em segundo plano:** clique em **Conceder** e ative o Sintoniza na lista. Se sua TV não tiver essa tela, ative o controle avançado (próximo item), que resolve sozinho.
- **Controle avançado (opcional):** necessário para setas, OK e voltar. Veja o passo a passo em [COMANDOS.md](COMANDOS.md#ativando-o-controle-avançado-uma-vez-só).

## 3. Pareie com a Alexa

O app mostra um código de 6 dígitos. Diga:

> "Alexa, pede pro Sintoniza parear com o código **4 8 2 9 1 3**"

Fale número por número. A tela da TV muda para **Pareado ✓**.

Na primeira vez, a Alexa ativa a skill sozinha. Se preferir, ative antes no app Alexa: **Mais → Skills e jogos → buscar "Sintoniza"**.

## 4. Use

> "Alexa, pede pro Sintoniza abrir o YouTube"

Todos os comandos estão em [COMANDOS.md](COMANDOS.md).

## Problemas comuns

| Sintoma | O que fazer |
|---|---|
| A Alexa diz que não conseguiu falar com a TV | Abra o app Sintoniza uma vez na TV (isso renova o registro) |
| O comando chega ("Último comando" muda), mas o app não abre | Falta a permissão de sobreposição. Veja o passo 2 |
| Navegação não funciona | Ative o controle avançado |
| A Alexa não entende o código | Fale número por número, devagar |
| O comando demora | Em repouso profundo, a TV pode levar alguns segundos para acordar |
