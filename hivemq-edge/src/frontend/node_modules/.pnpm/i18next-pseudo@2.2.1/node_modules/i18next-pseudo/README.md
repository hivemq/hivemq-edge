
[![npm package][npm-badge]][npm]

[npm-badge]: https://img.shields.io/npm/v/npm-package.png?style=flat-square
[npm]: https://www.npmjs.com/package/i18next-pseudo

[Example Website](http://mattboatmani18n.s3-website-us-east-1.amazonaws.com/)

# i18next-pseudo

i18next-pseudo is a plugin for [i18next](https://www.i18next.com/overview/plugins-and-utils) that returns translated strings in Pseudolocalization format.

# Pseudolocalization
[Pseudolocalization](https://en.wikipedia.org/wiki/Pseudolocalization)
> **Pseudolocalization** (or **pseudo-localization**) is a software testing method used for testing internationalization aspects of software. Instead of translating the text of the software into a foreign language, as in the process of localization, the textual elements of an application are replaced with an altered version of the original language.

`About => ḀḀḀḅṓṓṓṵṵṵṭ`

English in general is much shorter than other languages. This plugin will allow developers to design and develop without focusing on the length of an given word. Issues that arise during multilingual testing will be easily found at the time of development.

# Getting Started
`$ npm install --save i18next-pseudo`

# Usage
In your configuration for `i18next`, instantiate it as a new class and set the `postProcess` to `pseudo` in the `init` block.

```javascript
import i18n from 'i18next';
import { reactI18nextModule } from 'react-i18next';
import Pseudo from 'i18next-pseudo';

i18n
  .use(new Pseudo())
  .use(reactI18nextModule)
  .init({
    ...
    postProcess: ['pseudo'],
    ...
  });

export default i18n;

```

# Configuration
All configurations are optional.

| option | default | type | description |
| --- | --- | --- | --- |
| `languageToPseudo` | `en` |`string`| The locale that you would like to run through pseudolocalization |
| `repeatedLetters` | `['a', 'e', 'i', 'o', 'u', 'y', 'A', 'E', 'I', 'O', 'U', 'Y']` | `array of strings`| The letters that will be repeated in a given string |
| `letterMultiplier` | `2` | `number` | The amount of times you would like your repeatedLetter array to be multiplied |
| `letters` | `{  a: 'α',  b: 'ḅ',  c: 'ͼ',  d: 'ḍ',  e: 'ḛ',  f: 'ϝ',  g: 'ḡ',  h: 'ḥ',  i: 'ḭ',  j: 'ĵ',  : ḳ,  l: 'ḽ',  m: 'ṃ',  n: 'ṇ',  o: 'ṓ',  p: 'ṗ',  q: 'ʠ',  r: 'ṛ',  s: 'ṡ',  t: 'ṭ',  u: 'ṵ',  v: 'ṽ',  w: 'ẁ',  x: 'ẋ',  y: 'ẏ',  z: 'ẓ',  A: 'Ḁ',  B: 'Ḃ',  C: 'Ḉ',  D: 'Ḍ',  E: 'Ḛ',  F: 'Ḟ',  G: 'Ḡ',  H: 'Ḥ',  I: 'Ḭ',  : 'Ĵ',  K: 'Ḱ',  L: 'Ḻ',  M: 'Ṁ',  N: 'Ṅ',  O: 'Ṏ',  P: 'Ṕ',  Q: 'Ǫ',  R: 'Ṛ',  S: 'Ṣ',  T: 'Ṫ',  U: 'Ṳ',  V:   W: 'Ŵ',  X: 'Ẋ',  Y: 'Ŷ',  Z: 'Ż',}` | `object` | The letters in a string that will be replaced during pseudolocalization |
| `wrapped` | `false` | `boolean` | Should all strings be wrapped in to square brackets |
| `enabled` | `true` | `boolean`| Should pseudolocalization be ran (useful for environment variables)|

```javascript
i18n
  .use(new Pseudo({
    enabled: true,
    languageToPseudo: 'es-US',
    letterMultiplier: 4,
    repeatedLetters: ['B', 'o', 'a', 't']
  }))
  .use(reactI18nextModule)
  .init({
    ...
    postProcess: ['pseudo'],
    ...
  });

```
