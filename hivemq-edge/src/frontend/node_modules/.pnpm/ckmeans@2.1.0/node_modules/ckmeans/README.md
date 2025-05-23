# ckmeans

A javascript implementation of the [ckmeans](https://cran.r-project.org/web/packages/Ckmeans.1d.dp/index.html) algorithm. It's effectively a 1-dimensional version of kmeans, where a set of values are clustered into discrete groups. The algorithm has an `O(kn log(n))` runtime.

There is also a native version of this package if you need faster performance in node environments, see [ckmeans-native](https://www.npmjs.com/package/ckmeans-native)

For an alternative API used commonly in data visualization applications, check out [d3-scale-cluster](https://github.com/schnerd/d3-scale-cluster).

### Getting Started

##### Using npm

Install the npm package

```
npm install --save ckmeans
```

Load into your project

```es6
// Using ES6 imports
import ckmeans from 'ckmeans';

// Or, using require
var ckmeans = require('ckmeans');
```

You can also use something like [unpkg.com](https://unpkg.com/#/) to include it via `<script>` tag in the browser.

### Example Usage

This function returns the first value in each cluster

```js
var result = ckmeans([1, 2, 4, 5, 12, 43, 52, 123, 234, 1244], 6);
// [1, 12, 43, 123, 234, 1244]
```

### Thanks

Thanks to Haizhou Wang and Mingzhou Song for developing the original [Ckmeans 1D clustering algorithm](https://cran.r-project.org/web/packages/Ckmeans.1d.dp/)

### Contributing

```
yarn
yarn test  # run tests
yarn build # build distributable files
```

### Publishing

1. Build distributable file for browser: `yarn build`
2. Update CHANGELOG.md with changes in next version bump
3. Create new npm version: `npm version [major|minor|patch]`
4. Push to github with new version tag: `git push origin --tags`
5. Publish to npm: `npm publish`
