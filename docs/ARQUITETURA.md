# Arquitetura

```
┌─────────┐   voz    ┌───────┐  JSON   ┌──────────────────────────┐
│ Usuário │ ───────► │ Alexa │ ──────► │ Skill (Alexa-hosted,     │
└─────────┘          └───────┘         │ Node.js, alexa-skill/)   │
                                       └──────┬───────────┬───────┘
                                  lê/grava    │           │ envia push
                                              ▼           ▼
                                       ┌───────────┐  ┌─────────┐
                                       │ Firestore │  │   FCM   │
                                       └─────▲─────┘  └────┬────┘
                                 registra    │             │ data message
                                 token/código│             ▼
                                       ┌─────┴────────────────────┐
                                       │ App Android TV           │
                                       │ (android-app/)           │
                                       │ FirebaseMessagingService │
                                       │  └► CommandExecutor      │
                                       └──────────────────────────┘
```

Não há servidor próprio: a Amazon hospeda a skill e o Google entrega as mensagens. Como é a TV que recebe push (e não um servidor que conecta nela), não é preciso abrir portas no roteador.

## Modelo de dados (Firestore)

| Coleção | Documento | Quem escreve | Conteúdo |
|---|---|---|---|
| `devices` | `{uid}` (login anônimo da TV) | App: `fcmToken`, `model`, `appVersion`, `updatedAt`<br>Skill: `pairedAt` | Como alcançar a TV |
| `pairingCodes` | `{código de 6 dígitos}` | App cria, skill apaga | `uid`, `createdAt` |
| `links` | `{sha256(userId da Alexa)}` | Só a skill | `uid`, `linkedAt` |

As regras estão em [`firebase/firestore.rules`](../firebase/firestore.rules). A skill usa o Admin SDK (ignora as regras); o app só pode mexer no próprio `devices/{uid}` e criar códigos para si.

O `userId` da Alexa nunca é gravado em claro.

## Pareamento

1. O app faz login anônimo, publica o token FCM em `devices/{uid}` e cria `pairingCodes/{código}`.
2. O usuário fala o código. A skill, numa transação: lê o código, apaga (uso único), confere se tem menos de 10 minutos, grava `links/{hash}` e marca `devices/{uid}.pairedAt`.
3. O app observa `devices/{uid}`; quando `pairedAt` aparece, mostra "Pareado" e descarta o código.

Uma TV pode ser pareada com várias contas Alexa (botão "Parear outra Alexa"). "Desparear todas" apaga o registro e troca o `uid` da TV; os vínculos antigos viram órfãos e a skill os remove no próximo uso.

## Protocolo de comandos (v1)

A skill envia uma *data message* FCM de alta prioridade, com TTL de 30 s (comando atrasado é descartado), e um único campo `cmd` contendo JSON:

```json
{ "v": 1, "id": "uuid", "sentAt": 1730000000000, "type": "open_app", "app": "netflix", "name": "netflics" }
```

| `type` | Campos | Observação |
|---|---|---|
| `open_app` | `app` (id do catálogo ou `null`), `name` (texto falado) | O app tenta o pacote conhecido; se não achar, procura pelo nome no launcher |
| `youtube_search` | `query` | |
| `media` | `action`: `play` `pause` `next` `previous` `fast_forward` `rewind` | Evento de mídia para a sessão ativa |
| `volume` | `action`: `up` `down` (+`percent`), `set` (+`level` 0–100), `mute`, `unmute` | Volume do próprio stick |
| `key` | `key`: `up` `down` `left` `right` `ok` `back` `home`; `times` (1–10) | Requer controle avançado (ADB), exceto `home` |

Os mesmos ids existem em três lugares, e há testes que conferem se continuam iguais:
- `alexa-skill/skill-package/interactionModels/custom/pt-BR.json` (slots `APP_NAME` e `NAV_KEY`)
- `alexa-skill/lambda/src/commands.js`
- `android-app/.../command/Command.kt` e `AppCatalog.kt`

Mudanças incompatíveis no protocolo devem incrementar `v`. O app ignora versões que não conhece.

## Limitações do Android que moldam o app

- **Abrir apps em segundo plano** (Android 10+) exige a permissão "Sobrepor a outros apps". Em TVs sem essa tela, o controle avançado concede a permissão via `appops`.
- **Apertar teclas** (setas, OK, voltar) não é permitido pela API pública. Por isso o app se conecta ao ADB do próprio aparelho (`127.0.0.1:5555`, biblioteca [dadb](https://github.com/mobile-dev-inc/dadb)), depois que o usuário liga a "Depuração USB" e aceita o aviso.
- **Volume** é o do stick (`STREAM_MUSIC`). Em aparelhos com volume fixo via HDMI-CEC, o ajuste pode não ter efeito.
- **Ligar/desligar a TV** está fora do escopo.

## Segurança

- Só quem tem a chave da conta de serviço (o backend da skill) consegue enviar push para as TVs.
- A config do Firebase embutida no app é pública por natureza. A proteção está nas regras do Firestore.
- Códigos de pareamento têm 6 dígitos, uso único e validade de 10 minutos, e só existem enquanto a tela de pareamento está aberta.
- **Melhoria futura:** limitar tentativas de código erradas por usuário da Alexa.
