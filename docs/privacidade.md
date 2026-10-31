---
title: Política de Privacidade do Sintoniza
---

# Política de Privacidade do Sintoniza

*Última atualização: 31 de outubro de 2026*

O Sintoniza é um projeto open source ([código-fonte](https://github.com/WillxRv/sintoniza)) composto por uma skill da Alexa e um app para Android TV / Google TV. Ele permite controlar a TV por voz. Esta política explica quais dados são tratados, por quê e como apagá-los, nos termos da Lei Geral de Proteção de Dados (LGPD, Lei 13.709/2018).

## Dados que tratamos

**Da sua conta Alexa**
- Um identificador da sua conta na skill, fornecido pela Amazon, **armazenado apenas como hash criptográfico (SHA-256)**. Não temos acesso ao seu nome, e-mail, endereço ou outros dados da conta Amazon.
- A data em que a TV foi pareada.

**Do app na TV**
- Um identificador anônimo gerado pelo Firebase Authentication (login anônimo, sem e-mail ou senha).
- O token de notificação do Firebase Cloud Messaging, usado para entregar os comandos à TV.
- O fabricante e o modelo do aparelho, a versão do app e a data da última atualização desses dados.
- Códigos de pareamento temporários (6 dígitos), apagados assim que usados ou em até 10 minutos.

## Dados que não tratamos

- **Gravações de voz:** a Amazon processa sua voz. O Sintoniza recebe apenas o comando já interpretado (por exemplo, "abrir YouTube").
- **Histórico de comandos ou do que você assiste:** os comandos são entregues à TV e não são armazenados nos nossos servidores. O app guarda só o último comando, **localmente na TV**, para fins de diagnóstico.
- Localização, contatos, listas de apps instalados ou qualquer outro dado pessoal.

## Para que usamos

Exclusivamente para entregar os comandos que você pede à Alexa para a TV pareada com a sua conta. Não usamos os dados para publicidade, perfis ou qualquer outra finalidade. Não vendemos nem compartilhamos dados com terceiros.

## Onde os dados ficam

Os dados ficam no Google Firebase (Cloud Firestore e Firebase Cloud Messaging), que atua como operador, e a skill é hospedada pela Amazon (Alexa-hosted skills). Os dois serviços podem armazenar dados fora do Brasil, de acordo com as políticas de privacidade do [Google](https://policies.google.com/privacy) e da [Amazon](https://www.amazon.com.br/privacidade).

## Por quanto tempo e como apagar

Os dados ficam guardados enquanto a TV estiver pareada. Para apagar:
- **O vínculo com a Alexa:** diga *"Alexa, pede pro Sintoniza desparear"* e confirme, ou desative a skill no app Alexa.
- **Os dados da TV:** no app Sintoniza, use **Desparear todas** ou desinstale o app.

Códigos de pareamento expiram em 10 minutos. Registros órfãos (de TVs ou contas despareadas) são removidos automaticamente no uso seguinte.

## Crianças

O Sintoniza não é direcionado a crianças menores de 13 anos e não coleta intencionalmente dados delas.

## Seus direitos

Pela LGPD, você pode pedir acesso, correção ou exclusão dos seus dados, entre outros direitos. Na maioria dos casos as opções acima resolvem. Para qualquer outra solicitação ou dúvida, abra uma issue em [github.com/WillxRv/sintoniza/issues](https://github.com/WillxRv/sintoniza/issues).

## Mudanças nesta política

Mudanças serão publicadas nesta página, com a data de atualização acima. O histórico completo fica no repositório do projeto.
