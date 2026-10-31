// Gera os PNGs das lojas a partir dos SVGs desta pasta.
// Uso: cd art && npm install && npm run render
import { Resvg } from '@resvg/resvg-js';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';

const outputs = [
  // Alexa (loja de skills)
  { src: 'icon.svg', out: 'alexa-icon-108.png', width: 108 },
  { src: 'icon.svg', out: 'alexa-icon-512.png', width: 512 },
  // Google Play
  { src: 'icon.svg', out: 'play-icon-512.png', width: 512 },
  { src: 'banner.svg', out: 'play-tv-banner-1280x720.png', width: 1280 },
  { src: 'feature.svg', out: 'play-feature-1024x500.png', width: 1024 },
];

mkdirSync('out', { recursive: true });
for (const { src, out, width } of outputs) {
  const png = new Resvg(readFileSync(src), {
    fitTo: { mode: 'width', value: width },
    font: { loadSystemFonts: true, defaultFontFamily: 'Arial' },
  }).render().asPng();
  writeFileSync(`out/${out}`, png);
  console.log(`out/${out}`);
}
