# Sintoniza

Controle sua Android TV / Google TV (Mi TV Stick, Chromecast, TV boxes…) pela **Alexa**.

> "Alexa, pede pro Sintoniza abrir o YouTube"
> "Alexa, pede pro Sintoniza pausar"
> "Alexa, pede pro Sintoniza procurar receita de bolo no YouTube"

Projeto open source sob licença **AGPL-3.0**. Sem servidor próprio: usa uma skill *Alexa-hosted* (hospedada de graça pela Amazon) e o **Firebase** (plano gratuito) como ponte até o app na TV.

## Como funciona

```
Você fala ──► Alexa ──► Skill Sintoniza (Alexa-hosted)
                              │  Firebase Cloud Messaging (push)
                              ▼
                     App Sintoniza no Android TV ──► abre apps, pausa, volume, navega
```

1. O app na TV gera um **código de 6 dígitos**.
2. Você diz: *"Alexa, pede pro Sintoniza parear com o código 482913"*.
3. Pronto: a skill passa a mandar seus comandos para a sua TV.

Detalhes em [docs/ARQUITETURA.md](docs/ARQUITETURA.md).

## O que dá pra fazer

| Comando | Exemplo |
|---|---|
| Abrir apps | "abrir o YouTube", "coloca a Netflix", "abre o Globoplay" |
| Mídia | "pausar", "continuar", "próximo", "avançar", "retroceder" |
| Volume | "aumentar o volume", "volume 30", "mudo", "tirar o mudo" |
| Busca no YouTube | "procurar Jovem Nerd no YouTube" |
| Navegação* | "pra baixo", "três vezes pra direita", "ok", "voltar", "início" |

\* Navegação precisa ativar o *controle avançado* (depuração ADB) uma vez. Veja [docs/COMANDOS.md](docs/COMANDOS.md).

Ligar/desligar a TV **não** está no escopo (depende de HDMI-CEC ou infravermelho).

## Estrutura

| Pasta | O que é |
|---|---|
| [`android-app/`](android-app) | App para Android TV (Kotlin + Jetpack Compose for TV) |
| [`alexa-skill/`](alexa-skill) | Skill Alexa-hosted (Node.js) + modelo de interação pt-BR |
| [`firebase/`](firebase) | Regras de segurança do Firestore |
| [`docs/`](docs) | Arquitetura, instalação, auto-hospedagem |

## Usar

- **Usuário final:** [docs/INSTALACAO.md](docs/INSTALACAO.md)
- **Hospedar a sua própria instância / publicar:** [docs/AUTO-HOSPEDAGEM.md](docs/AUTO-HOSPEDAGEM.md)

## Licença

[AGPL-3.0-or-later](LICENSE). Você pode usar, modificar e até vender, mas qualquer versão modificada distribuída ou oferecida como serviço precisa ter o código-fonte aberto sob a mesma licença.

"Sintoniza" é o nome da instância oficial. Forks públicos devem usar outro nome e outro nome de invocação na Alexa.
