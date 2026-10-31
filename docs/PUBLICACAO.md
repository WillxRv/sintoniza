# Publicação

Passo a passo para publicar a instância oficial: o APK no GitHub, a política de privacidade no GitHub Pages e a skill na loja da Alexa. Tudo grátis (veja [custos](#custos)).

## 0. Antes de tudo: segurança

- [ ] **Trocar a chave do Firebase**, caso ela tenha sido exposta. No Google Cloud, abra **IAM → Contas de serviço → `firebase-adminsdk-fbsvc` → Chaves**, exclua a chave antiga e gere uma nova. Salve direto como `alexa-skill/lambda/firebase-service-account.json`, gere o zip de novo (passo 3) e reimporte na Alexa-hosted.
- [ ] **Backup da chave de assinatura do app:** guarde `android-app/sintoniza-release.jks` e `android-app/keystore.properties` num lugar seguro (gerenciador de senhas, pendrive). **Sem eles não é possível publicar atualizações do app**: os usuários teriam que desinstalar e instalar de novo.
- [ ] Apagar zips antigos em `alexa-skill/dist/` depois de importar, porque eles contêm a chave do Firebase.

## 1. Política de privacidade (GitHub Pages)

1. No GitHub, abra o repositório e vá em **Settings → Pages**.
2. Em **Build and deployment**, escolha **Source: Deploy from a branch**, **Branch: `main`** e a pasta **`/docs`**. Clique em **Save**.
3. Em 1 ou 2 minutos, a página fica no ar em:
   **https://willxrv.github.io/sintoniza/privacidade.html**
4. Abra o link e confira.

## 2. APK no GitHub Releases

O APK de release (assinado e otimizado, cerca de 4 MB) é gerado com:

```bash
./gradlew assembleRelease
```

Ele sai em `android-app/app/build/outputs/apk/release/app-release.apk`.

1. No GitHub, vá em **Releases → Draft a new release**.
2. **Tag:** `v1.0.2`. **Título:** `Sintoniza 1.0.2`.
3. Anexe o APK **renomeado para `sintoniza.apk`**. O nome importa: o link abaixo sempre aponta para a versão mais recente.
4. Clique em **Publish release**.

Link fixo para baixar a versão mais recente (é o que o usuário digita no app Downloader):

**https://github.com/WillxRv/sintoniza/releases/latest/download/sintoniza.apk**

## 3. Skill: código e modelo atualizados

1. Gere o zip de importação, que inclui a chave do Firebase. No PowerShell, na raiz do repositório, rode `.\alexa-skill\pack.ps1`. O zip sai em `alexa-skill/dist/sintoniza-lambda.zip`.
2. No console da Alexa, vá em **Code → Import Code → Deploy**.
3. Vá em **Build → JSON Editor**, cole `alexa-skill/skill-package/interactionModels/custom/pt-BR.json` e clique em **Save → Build skill**.

## 4. Skill: aba Distribution

### Skill Preview

| Campo | Valor |
|---|---|
| Public Name | Sintoniza |
| One Sentence Description | Controle sua Android TV ou Google TV pela Alexa: abra apps, pause, ajuste o volume e navegue. |
| Detailed Description | *(texto abaixo)* |
| What's new? | Primeira versão. |
| Example Phrases | `Alexa, abrir sintoniza` · `Alexa, pede pro sintoniza abrir o youtube` · `Alexa, pede pro sintoniza pausar` |
| Small Skill Icon (108×108) | `art/out/alexa-icon-108.png` |
| Large Skill Icon (512×512) | `art/out/alexa-icon-512.png` |
| Category | Smart Home (ou Utilities) |
| Keywords | tv, android tv, google tv, controle remoto, youtube, netflix, mi stick, chromecast |
| Privacy Policy URL | https://willxrv.github.io/sintoniza/privacidade.html |
| Terms of Use URL | *(opcional; pode deixar vazio)* |

**Detailed Description:**

```
O Sintoniza conecta a Alexa à sua Android TV ou Google TV (Mi TV Stick, Chromecast com Google TV, TV boxes e TVs com Android).

COMO COMEÇAR
1. Instale o app Sintoniza na TV. No app Downloader, digite: github.com/WillxRv/sintoniza/releases/latest/download/sintoniza.apk
2. Abra o app e anote o código de 6 dígitos.
3. Diga: "Alexa, pede pro Sintoniza parear com o código" e fale o código.

O QUE DÁ PRA FAZER
• Abrir apps: "Alexa, pede pro Sintoniza abrir o YouTube" (também Netflix, Globoplay, Prime Video, Disney+, Max, Spotify e outros)
• Buscar no YouTube: "Alexa, pede pro Sintoniza procurar receita de bolo no YouTube"
• Mídia: pausar, continuar, avançar, retroceder, próximo
• Volume: aumentar, abaixar, "volume 30", mudo
• Navegar nos menus: cima, baixo, esquerda, direita, ok, voltar (requer ativar o controle avançado no app)

Dica: diga "Alexa, abrir Sintoniza" para mandar vários comandos seguidos sem repetir o nome.

Projeto open source (AGPL-3.0): github.com/WillxRv/sintoniza
```

### Privacy & Compliance

| Pergunta | Resposta |
|---|---|
| Does this skill allow users to make purchases or spend real money? | **No** |
| Does this Alexa skill collect users' personal information? | **No** (só um identificador anônimo com hash; nada de nome, e-mail, endereço etc.) |
| Is this skill directed to or does it target children under the age of 13? | **No** |
| Does this skill contain advertising? | **No** |
| Export Compliance | ✅ marcado |
| Testing Instructions | *(texto abaixo)* |

**Testing Instructions** (em inglês, para o time de certificação):

```
Sintoniza is a remote control for Android TV / Google TV devices. It requires the companion
Android TV app (open source, free): https://github.com/WillxRv/sintoniza/releases/latest

Without a paired TV, every command explains how to pair and no command is sent:
- "Alexa, open sintoniza" -> explains pairing and asks for the code
- "Alexa, ask sintoniza to open youtube" -> says the TV is not paired yet

Pairing: the TV app shows a 6-digit code; the user says "Alexa, ask sintoniza to pair with code
<code>". Codes expire after 10 minutes and are single-use.

Unpairing asks for confirmation ("desparear" -> "tem certeza?" -> yes/no).

Demo video of the full flow (pairing, opening apps, playback, volume, navigation):
<COLE AQUI O LINK DO VÍDEO NÃO LISTADO>

Skill locale is pt-BR; sample utterances: "abrir o youtube", "pausar", "volume 30",
"procurar foo fighters no youtube", "pra baixo", "ok".
```

### Availability

- **Who should have access:** Public
- **Countries and regions:** Brazil (ou "In all countries and regions where Amazon distributes skills")
- **Pre-release (beta):** opcional. Dá para convidar até 500 pessoas por e-mail antes de publicar.

## 5. Vídeo para o revisor

Um vídeo de 1 a 2 minutos, gravado com o celular apontado para a TV e o Echo, postado no YouTube como **não listado**. Roteiro:

1. Mostre o app Sintoniza aberto na TV, com o código na tela.
2. *"Alexa, pede pro Sintoniza parear com o código …"*, e a TV mostra "Pareado".
3. *"Alexa, pede pro Sintoniza abrir o YouTube"*
4. *"Alexa, pede pro Sintoniza procurar Foo Fighters no YouTube"*
5. *"Alexa, abrir Sintoniza"* → *"baixo"* → *"direita"* → *"ok"* (o vídeo começa)
6. *"pausar"* → *"continuar"* → *"volume 20"*
7. *"Alexa, pede pro Sintoniza desparear"* → *"não"*

## 6. Certificação

1. Vá em **Certification → Validation**, clique em **Run** e corrija o que aparecer.
2. Vá em **Certification → Functional test**, clique em **Run**.
3. Clique em **Submission → Certify and publish now** (ou *Certify now and publish later*).

A revisão leva de alguns dias a duas semanas. Se reprovarem, o e-mail explica o motivo. Corrija e reenvie.

Possíveis pontos de questionamento:
- **Nome de invocação de uma palavra** ("sintoniza"): é aceito por ser o nome da marca. Se pedirem, troque para "tv sintoniza".
- **Dependência de hardware:** o vídeo e as instruções de teste resolvem.

## Custos

| Item | Custo |
|---|---|
| Conta Amazon Developer, certificação, Alexa-hosted | Grátis |
| Firebase (plano Spark, sem cobrança configurada) | Grátis. Se estourar a cota, o serviço para até o dia seguinte; **nada é cobrado** |
| GitHub (repositório, Pages, Releases) | Grátis |
| Google Play (opcional) | US$ 25, pagamento único |

## Depois de publicar

- **Atualizar o app:** aumente `versionCode` e `versionName` em `android-app/app/build.gradle.kts`, gere o release e publique uma nova Release no GitHub, com o mesmo nome `sintoniza.apk`.
- **Atualizar a skill:** as mudanças de **código** (Deploy) valem na hora. Mudanças no **modelo de interação** ou na **loja** exigem uma nova certificação.
