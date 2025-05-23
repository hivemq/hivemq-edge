# d3-scale-cluster

A custom D3 scale powered by a 1-dimensional clustering algorithm. Similar to [quantile scales](https://github.com/d3/d3-scale/blob/master/README.md#scaleQuantile), the cluster scale maps a continuous input domain to a discrete range. The number of values in the output range determines the number of clusters that will be computed from the domain. The graphic below demonstrates how cluster compares to D3's quantile and quantize scales:

<img width="420" alt="d3 scale cluster example" src="https://cloud.githubusercontent.com/assets/875591/18608070/0213d7ce-7cdf-11e6-89aa-1b0e18e63cc8.png">

You can also check out the ["Choropleth with d3-scale-cluster"](https://bl.ocks.org/schnerd/99767e64051096388078913afca3ff4e) block for an interactive comparison of cluster, quantile, and quantize scales.

Clusters are computed using a 1-dimensional clustering algorithm with an `O(kn log(n))` runtime (where `k` is the number of clusters desired). This should be [fast enough for the majority of data sets](https://cloud.githubusercontent.com/assets/875591/19367754/5159b53e-9151-11e6-9fee-52ce88cdf696.png), but it's worth doing your own performance testing.

For more details on this project and the underlying clustering algorithm, please read this blog post on Medium: ["Using clustering to create a new D3.js color scale"](https://medium.com/@dschnr/using-clustering-to-create-a-new-d3-js-color-scale-dec4ccd639d2)

For more direct access to the ckmeans algorithm (not as a D3 scale), check out [ckmeans](https://www.npmjs.com/package/ckmeans) or its native sibling [ckmeans-native](https://www.npmjs.com/package/ckmeans-native).

### Getting Started

##### Using npm

Install the npm package

```
npm install --save d3-scale-cluster
```

Load the scale into your project

```es6
// Using ES6 imports
import scaleCluster from 'd3-scale-cluster';

// Or, using require
var scaleCluster = require('d3-scale-cluster');
```

##### Using a `<script>` tag

Include the following script tag on your page after D3

```html
<script src="https://unpkg.com/d3-scale-cluster@1.3.1/dist/d3-scale-cluster.min.js"></script>
```

Reference the scale directly from the d3 object

```es6
var scale = d3.scaleCluster();
```

### Example Usage

This scale largely has the same API as [d3.scaleQuantile](https://github.com/d3/d3-scale/blob/master/README.md#scaleQuantile) (however we use `clusters()` instead of `quantiles()`)

```js
var scale = d3
  .scaleCluster()
  .domain([1, 2, 4, 5, 12, 43, 52, 123, 234, 1244])
  .range(['#E5D6EA', '#C798D3', '#9E58AF', '#7F3391', '#581F66', '#30003A']);

var clusters = scale.clusters(); // [12, 43, 123, 234, 1244]
var color = scale(52); // '#9E58AF'
var extent = scale.invertExtent('#9E58AF'); // [43, 123]
```

### API

d3.**scaleCluster**()

Constructs a new cluster scale with an empty domain and an empty range. The cluster scale is invalid until both a domain and range are specified.

_cluster_(_value_)

Given a value in the input domain, returns the corresponding value in the output range.

_cluster_.**invertExtent**(_value_)

Returns the extent of values in the domain [x0, x1] for the corresponding value in the range: the inverse of cluster. This method is useful for interaction, say to determine the value in the domain that corresponds to the pixel location under the mouse.

_cluster_.**domain**(_[domain]_)

If domain is specified, sets the domain of the quantile scale to the specified set of discrete numeric values. The array must not be empty, and must contain at least one numeric value; NaN, null and undefined values are ignored and not considered part of the sample population. If the elements in the given array are not numbers, they will be coerced to numbers. A copy of the input array is sorted and stored internally. If domain is not specified, returns the scale’s current domain.

_cluster_.**range**(_[range]_)

If range is specified, sets the discrete values in the range. The array must not be empty, and may contain any type of value. The number of values in (the cardinality, or length, of) the range array determines the number of clusters that are computed. If range is not specified, returns the current range.

_cluster_.**clusters**()

Returns the cluster thresholds. If the range contains n discrete values, the returned array will contain n - 1 thresholds. Values less than the first threshold are considered in the first cluster; values greater than or equal to the first threshold but less than the second threshold are in the second cluster, and so on.

_cluster_.**copy**()

Returns an exact copy of this scale. Changes to this scale will not affect the returned scale, and vice versa.

_cluster_.**import**()

Updates the scale with the result of a _cluster_.**export**() call. Useful for offloading computation into a webworker.

_cluster_.**export**()

Exports the internals of the scale as an object, for use with _cluster_.**import**(). Useful for offloading computation into a webworker.

### Using in a Web Worker

For data sets of significant size, you may want to offload computation into a Web Worker so that it does not block the main thread. You can use _cluster_.**import**() and _cluster_.**export**() as follows:

**worker.js**

```js
const scale = scaleCluster().domain(domain).range(range);
self.postMessage({scale: scale.export()});
```

**Main thread**

```js
worker.onmessage = function (event) {
  const scale = scaleCluster().import(event.data.scale);
};
```

### Thanks

Thanks to Haizhou Wang and Mingzhou Song for developing the original [Ckmeans 1D clustering algorithm](https://cran.r-project.org/web/packages/Ckmeans.1d.dp/), and Tom MacWright for his [previous work](http://www.macwright.org/2013/02/18/literate-jenks.html) in bringing these techniques to the web.

### Links & Resources

- [Using clustering to create a new D3.js color scale](https://medium.com/@dschnr/using-clustering-to-create-a-new-d3-js-color-scale-dec4ccd639d2) - Medium post describing this project
- [Choropleth with d3-scale-cluster](https://bl.ocks.org/schnerd/99767e64051096388078913afca3ff4e) - Interactive block comparing cluster, quantile, and quantize scales
- [Interactive d3-scale-cluster demo](http://bl.ocks.org/tomshanley/raw/2de81c66fbe4cab9dc4e4e4c579a4d1a/) - Paste in your data to see clusters. By [@tomshanleynz](https://twitter.com/tomshanleynz)

### Contributing

```
npm install
npm run test  # run tests
npm run build # build distributable file
```

### Publishing

1. Build distributable file for browser: `npm run build`
2. Update CHANGELOG.md with changes in next version bump
3. Update slated version in unpkg url in README.md
4. Create new npm version: `npm version [major|minor|patch]`
5. Push to github with new version tag: `git push origin --tags`
6. Publish to npm: `npm publish`
