<h4 align="center">
    <a href="https://github.com/ahmadawais/cli-check-node">
        <img src="https://github.com/ahmadawais/cli-check-node/raw/master/.github/cli-check-node.jpg" alt="cli-check-node" />
    </a>
    <br>
    <br>

Check the installed Node.js version and exit as per the required Node.js version.

[![DOWNLOADS](https://img.shields.io/npm/dt/cli-check-node?label=DOWNLOADS&colorA=6ABB47&colorB=6ABB47&style=flat)](https://www.npmjs.com/package/cli-check-node) [![Learn Node.js CLI Automation](https://img.shields.io/badge/-NodeCLI.com%20%E2%86%92-gray.svg?colorB=6ABB47&style=flat)](https://nodecli.com/?utm_source=GitHubFOSS) [![Follow @MrAhmadAwais on Twitter](https://img.shields.io/badge/FOLLOW%20@MRAHMADAWAIS%20%E2%86%92-gray.svg?colorA=6ABB47&colorB=6ABB47&style=flat)](https://twitter.com/mrahmadawais/)

</h4>

<br>

# cli-check-node

- ✅ Checks if a Node app is running on the right Node version
- ❌ Fails with a message to update Node version to specified version

<br>

[![📟](https://raw.githubusercontent.com/ahmadawais/stuff/master/images/git/install.png)](./../../)

## Install

```sh
npm install cli-check-node
```

<br>

[![⚙️](https://raw.githubusercontent.com/ahmadawais/stuff/master/images/git/usage.png)](./../../)

## Usage

```js
const checkNode = require('cli-check-node');
checkNode('13'); // Fails the script if current node version is less than 13.
```

<br />

[![📃](https://raw.githubusercontent.com/ahmadawais/stuff/master/images/git/options.png)](./../../)

## API

### checkNode(requiredNodeVersion, options?)

#### ❯ requiredNodeVersion

Type: `string`<br>

#### ❯ options

Type: `object`<br>
Default: `{fail: true}` (optional)

You can specify the options below.

#### ❯ fail

Type: `boolean`<br>
Default: `true` (optional)

```js
checkNode('13', {fail: false});
// Notifies if current node version is less than 13. Does NOT fail.
```

<br>

[![📝](https://raw.githubusercontent.com/ahmadawais/stuff/master/images/git/log.png)](changelog.md)

## Changelog

[❯ Read the changelog here →](changelog.md)

<br>

<small>**KEY**: `📦 NEW`, `👌 IMPROVE`, `🐛 FIX`, `📖 DOC`, `🚀 RELEASE`, and `✅ TEST`

> _I use [Emoji-log](https://github.com/ahmadawais/Emoji-Log), you should try it and simplify your git commits._

</small>

<br>

[![📃](https://raw.githubusercontent.com/ahmadawais/stuff/master/images/git/license.png)](./../../)

## License & Conduct

- MIT © [Ahmad Awais](https://twitter.com/MrAhmadAwais/)
- [Code of Conduct](code-of-conduct.md)

<br>

[![🙌](https://raw.githubusercontent.com/ahmadawais/stuff/master/images/git/connect.png)](./../../)

## Connect

<div align="left">
    <p><a href="https://github.com/ahmadawais"><img alt="GitHub @AhmadAwais" align="center" src="https://img.shields.io/badge/GITHUB-gray.svg?colorB=6cc644&colorA=6cc644&style=flat" /></a>&nbsp;<small><strong>(follow)</strong> To stay up to date on free & open-source software</small></p>
    <p><a href="https://twitter.com/MrAhmadAwais/"><img alt="Twitter @MrAhmadAwais" align="center" src="https://img.shields.io/badge/TWITTER-gray.svg?colorB=1da1f2&colorA=1da1f2&style=flat" /></a>&nbsp;<small><strong>(follow)</strong> To get #OneDevMinute daily hot tips & trolls</small></p>
    <p><a href="https://www.youtube.com/AhmadAwais"><img alt="YouTube AhmadAwais" align="center" src="https://img.shields.io/badge/YOUTUBE-gray.svg?colorB=ff0000&colorA=ff0000&style=flat" /></a>&nbsp;<small><strong>(subscribe)</strong> To tech talks & #OneDevMinute videos</small></p>
    <p><a href="https://AhmadAwais.com/"><img alt="Blog: AhmadAwais.com" align="center" src="https://img.shields.io/badge/MY%20BLOG-gray.svg?colorB=4D2AFF&colorA=4D2AFF&style=flat" /></a>&nbsp;<small><strong>(read)</strong> In-depth & long form technical articles</small></p>
    <p><a href="https://www.linkedin.com/in/MrAhmadAwais/"><img alt="LinkedIn @MrAhmadAwais" align="center" src="https://img.shields.io/badge/LINKEDIN-gray.svg?colorB=0077b5&colorA=0077b5&style=flat" /></a>&nbsp;<small><strong>(connect)</strong> On the LinkedIn profile y'all</small></p>
</div>

<br>

[![👌](https://raw.githubusercontent.com/ahmadawais/stuff/master/images/git/sponsor.png)](./../../)

## Sponsor

Me ([Ahmad Awais](https://twitter.com/mrahmadawais/)) and my incredible wife ([Maedah Batool](https://twitter.com/MaedahBatool/)) are two engineers who fell in love with open source and then with each other. You can read more [about me here](https://ahmadawais.com/about). If you or your company use any of my projects or like what I’m doing then consider backing me. I'm in this for the long run. An open-source developer advocate.

- 🌟  **$9.99/month (recommended)** ❯ [Two cups of Caffè latte (coffee) every month →](https://pay.paddle.com/checkout/540217)
- 🚀  **$99.99 (one-time sponsor)** ❯ [Support an hour of open-source code →](https://pay.paddle.com/checkout/515568)
- 🔰  **$49.99 (one-time sponsor)** ❯ [Support an hour of maintenance →](https://pay.paddle.com/checkout/527253)
- ☕️  **$9.99 (one-time sponsor)** ❯ [Lunch/coffee →](https://pay.paddle.com/checkout/527254)

<br>

Or you can back me by checking out my super fun video course. As developers, we spend over 200 Hrs/month with our code editors — it's only fair to learn your next editor deeply. This course will save you 15-20 hours every month.  <a href="https://vscode.pro/?utm_source=GitHubFOSS" target="_blank">Become a VSCode Power User</a> →</p>

<a href="https://vscode.pro/?utm_source=GitHubFOSS" target="_blank"><img src="https://raw.githubusercontent.com/ahmadawais/stuff/master/images/vscodepro/VSCode.jpeg" /><br><strong>VSCODE</strong></a>

<br>

[![VSCode](https://img.shields.io/badge/-VSCode.pro%20%E2%86%92-gray.svg?colorB=4D2AFF&style=flat)](https://VSCode.pro/?utm_source=GitHubFOSS)
[![Ahmad on Twitter](https://img.shields.io/twitter/follow/mrahmadawais.svg?style=social&label=Follow%20@MrAhmadAwais)](https://twitter.com/mrahmadawais/)
