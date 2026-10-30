# Hospedando a sua instância

Este guia serve tanto para a **instância oficial** quanto para quem quer **hospedar a própria** (fork ou uso pessoal). Custo: zero, dentro das cotas gratuitas do Firebase e da Alexa-hosted.

Você vai precisar de:
- Conta Google (Firebase)
- Conta **Amazon Developer** ([developer.amazon.com](https://developer.amazon.com)), **com o mesmo e-mail da sua Alexa** para testar nos seus Echos
- [Android Studio](https://developer.android.com/studio) para compilar o APK
- Node.js 18+ (opcional, para rodar os testes da skill)

> **Fork público?** Use outro nome de app e outro **nome de invocação** (por exemplo "minha tv"), para não conflitar com a skill oficial "Sintoniza".

---

## 1. Firebase

1. Em [console.firebase.google.com](https://console.firebase.google.com), crie um projeto. O Google Analytics é opcional.
2. **Authentication → Método de login → Anônimo → Ativar.**
3. **Firestore Database → Criar banco** (modo produção). Para o Brasil, a região `southamerica-east1` dá menor latência.
4. **Publique as regras.** Escolha um dos jeitos:
   - Pelo console: **Firestore → Regras**, cole o conteúdo de [`firebase/firestore.rules`](../firebase/firestore.rules) e clique em Publicar.
   - Pelo [Firebase CLI](https://firebase.google.com/docs/cli): rode o comando abaixo dentro de `firebase/`.
     ```bash
     firebase deploy --only firestore:rules --project SEU_PROJETO
     ```
5. **Adicione um app Android** (ícone do Android na página do projeto), com o pacote `app.sintoniza` (ou o seu, se mudou). Não é preciso baixar o `google-services.json`: anote os valores da tela de configuração ou de **Configurações do projeto → Seus apps**.
6. **Gere a chave do backend:** **Configurações do projeto → Contas de serviço → Gerar nova chave privada.** Isso baixa um JSON.
   > ⚠️ **Esse arquivo é o único segredo do projeto.** Quem tiver ele consegue mandar comandos para qualquer TV da sua instância. Nunca coloque no Git (o `.gitignore` já bloqueia `firebase-service-account.json`).

## 2. App Android TV

1. Copie `android-app/firebase.properties.example` para `android-app/firebase.properties` e preencha com os valores do passo 1.5:

   | Campo | Onde achar no Firebase |
   |---|---|
   | `firebase.projectId` | ID do projeto |
   | `firebase.applicationId` | ID do app (`1:…:android:…`) |
   | `firebase.apiKey` | Chave de API da Web |
   | `firebase.gcmSenderId` | Código do remetente / número do projeto |

   Esses valores **não são segredo** (qualquer APK publicado os expõe). Na instância oficial, este arquivo pode ir para o Git.
2. Abra a pasta `android-app/` no Android Studio e espere o Gradle sincronizar.
3. Para gerar o APK: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. **Instale na TV** (com a depuração USB ligada, veja [COMANDOS.md](COMANDOS.md#ativando-o-controle-avançado-uma-vez-só)). Primeiro conecte ao IP da TV:
   ```bash
   adb connect IP_DA_TV:5555
   ```
   Depois instale o APK:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   Ou rode direto pelo botão ▶ do Android Studio, com a TV conectada.

### Trocar de servidor sem recompilar

Um APK já instalado (inclusive o oficial) pode apontar para o seu Firebase:

```bash
adb shell am start -n app.sintoniza/.ui.MainActivity --es server_config '{"projectId":"meu-projeto","applicationId":"1:123:android:abc","apiKey":"AIza...","gcmSenderId":"123"}'
```

A TV pede confirmação antes de trocar. Para voltar ao oficial, use o botão **Usar servidor oficial**.

## 3. Skill da Alexa (Alexa-hosted)

1. Em [developer.amazon.com/alexa/console/ask](https://developer.amazon.com/alexa/console/ask), clique em **Create Skill**:
   - **Name:** Sintoniza (ou o seu nome)
   - **Primary locale:** Portuguese (BR)
   - **Type:** Other → **Custom**
   - **Hosting:** **Alexa-hosted (Node.js)**
   - **Template:** Start from scratch
2. **Modelo de interação:** na aba **Build**, vá em **Interaction Model → JSON Editor**, cole o conteúdo de [`alexa-skill/skill-package/interactionModels/custom/pt-BR.json`](../alexa-skill/skill-package/interactionModels/custom/pt-BR.json), clique em **Save** e depois em **Build skill**.
   - Se for um fork, troque `"invocationName": "sintoniza"` pelo seu.
3. **Código:** na aba **Code**, dentro de `lambda/`:
   - substitua `index.js` e `package.json` pelos deste repositório;
   - crie a pasta `src/` com os arquivos de [`alexa-skill/lambda/src/`](../alexa-skill/lambda/src);
   - crie `firebase-service-account.json` e cole o JSON baixado no passo 1.6;
   - clique em **Save** e depois em **Deploy**.

   A pasta `test/` não precisa ir para a Alexa.
4. **Teste:** na aba **Test**, mude de *Off* para **Development**. Digite ou fale "abrir sintoniza". A partir daí, a skill também funciona nos Echos da sua conta.

> Também dá para versionar o código da Alexa-hosted com git ou com o [ASK CLI](https://developer.amazon.com/docs/smapi/quick-start-alexa-skills-kit-command-line-interface.html), em vez de colar arquivos no console.

### Rodar os testes da skill localmente

Dentro de `alexa-skill/lambda/`, instale as dependências e rode os testes:

```bash
npm install
```

```bash
npm test
```

Os testes usam um backend falso. Não precisam de Firebase nem de conta Amazon.

## 4. Primeiro teste ponta a ponta

1. Abra o app na TV e anote o código.
2. Diga: "Alexa, pede pro Sintoniza parear com o código …".
3. Diga: "Alexa, pede pro Sintoniza abrir o YouTube".
4. Se não funcionar:
   - O painel **Último comando** do app mostra se a mensagem chegou.
   - Veja os logs da skill em **Code → CloudWatch Logs**.
   - Veja os logs do app (filtro `Sintoniza`) no Logcat do Android Studio.

## 5. Distribuindo para outras pessoas

| Etapa | Para quem | Certificação? |
|---|---|---|
| **Development** | Echos da sua conta | Não |
| **Beta test** (aba Distribution → Availability) | Até 500 convidados por e-mail | Não |
| **Publicação** | Qualquer pessoa, pela loja de skills | Sim |

O que a certificação pede:
- **Política de privacidade** em URL pública (por exemplo, GitHub Pages). Ela precisa dizer que a skill guarda um identificador anônimo da conta Alexa (com hash) e o token de notificação da TV, só para entregar os comandos.
- Ícones 108×108 e 512×512, a descrição e as frases de exemplo (já preenchidas em `skill-package/skill.json`).
- **Instruções de teste.** O revisor não terá sua TV. Explique isso e, se possível, mande um vídeo mostrando o funcionamento.
- O nome de invocação não pode conter marcas de terceiros.

Para o app, publicar na Play Store (seção Android TV) exige uma conta de desenvolvedor Google (US$ 25, pagamento único). A revisão de TV pede banner, navegação por controle remoto e uma justificativa do uso da permissão de sobreposição.
