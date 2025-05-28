# Changelog

### 2.0.0

- Update ckmeans package to remove native code dependency, check out the ckmeans-native package if want a faster native version of this algorithm.

### 1.3.1

- Split ckmeans into its own package

### 1.3.0

- Significant perf improvements in Node via NAPI (see #5, thanks @furstenheim)

### 1.2.0

- Add helpers for computation in web worker (thanks @furstenheim)

### 1.1.7

- Add support for RequireJS/AMD (thanks @kimxogus)

### 1.1.0

- Ported [Ckmeans 3.4.6](https://cran.r-project.org/web/packages/Ckmeans.1d.dp/) to JavaScript for a [~10x performance boost](https://cloud.githubusercontent.com/assets/875591/19367940/67688548-9152-11e6-9c2e-01e3e800bb65.png)

### 1.0.1

- Initial release
