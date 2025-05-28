var ckmeans = require('ckmeans');

function d3scaleCluster() {
  var isReady = false;
  var domain = [];
  var range = [];
  var breakpoints = [];

  var scale = function (x) {
    if (!isReady) return undefined;

    for (var i = breakpoints.length - 1; i >= 0; i--) {
      if (x >= breakpoints[i]) {
        return range[i];
      }
    }
    return range[0];
  };

  function rescale() {
    if (range.length <= 2) {
      return;
    }

    var clusters = ckmeans(domain, Math.min(domain.length, range.length));
    isReady = clusters.length !== 0;
    breakpoints = [];
    for (var i = 0; i < clusters.length; i++) {
      // clusters might be a typed array
      breakpoints.push(clusters[i]);
    }
  }

  scale.domain = function () {
    if (arguments.length) {
      domain = arguments[0];
      rescale();
      return scale;
    } else {
      return domain;
    }
  };

  scale.range = function () {
    if (arguments.length) {
      var newRange = arguments[0];
      var needsUpdate = newRange.length !== range.length;
      range = newRange;
      if (needsUpdate) {
        rescale();
      }
      return scale;
    } else {
      return range;
    }
  };

  scale.invertExtent = function (rangeValue) {
    // d3.scaleQuantile.invertExtent returns NaNs for invalid usage, emulate that behavior here
    var extentA = NaN;
    var extentB = NaN;
    for (var i = 0; i < range.length; i++) {
      if (range[i] === rangeValue) {
        extentA = breakpoints[i];
        extentB = i + 1 < range.length ? breakpoints[i + 1] : NaN;
        break;
      }
    }
    return [extentA, extentB];
  };

  scale.clusters = function () {
    return breakpoints.slice(1);
  };

  scale.export = function () {
    return {
      isReady: isReady,
      domain: domain,
      range: range,
      breakpoints: breakpoints,
    };
  };

  scale.import = function (params) {
    if (!params) {
      throw new Error('Import requires parameters');
    }
    isReady = params.isReady;
    domain = params.domain;
    range = params.range;
    breakpoints = params.breakpoints;
    return scale;
  };

  scale.copy = function () {
    return d3scaleCluster().domain(domain).range(range);
  };

  return scale;
}

if (typeof d3 === 'object') {
  // eslint-disable-next-line no-undef
  d3.scaleCluster = d3scaleCluster;
}

module.exports = d3scaleCluster;
