# mqtt-match

> Match mqtt formatted topic strings to strings

[![npm](https://img.shields.io/npm/v/mqtt-match.svg)](https://www.npmjs.com/package/mqtt-match)
![Node version](https://img.shields.io/node/v/mqtt-match.svg)
[![Node.js CI](https://github.com/ralphtheninja/mqtt-match/actions/workflows/node.js.yml/badge.svg)](https://github.com/ralphtheninja/mqtt-match/actions/workflows/node.js.yml)
[![JavaScript Style Guide](https://img.shields.io/badge/code_style-standard-brightgreen.svg)](https://standardjs.com)

## Usage

```js
const match = require('mqtt-match')
console.log(match('foo/+', 'foo/bar'))
// true
```

## Api

### `match(filter, topic[, handleSharedSubscription])`

* `filter` (string) - mqtt filter topic, e.g. `foo/+/bar`
* `topic` (string) - topic string, e.g. `foo/314/bar`
* `handleSharedSubscription` (boolean) - set to true if handling `$share/` filter topics

## License
MIT
