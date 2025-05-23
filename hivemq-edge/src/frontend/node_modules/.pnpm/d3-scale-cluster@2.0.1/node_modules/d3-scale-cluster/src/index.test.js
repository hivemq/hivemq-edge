const d3scaleCluster = require('./index');

describe('Scale', function () {
  var DEFAULT_DOMAIN = [1, 2, 4, 5, 12, 43, 52, 123, 234, 1244];
  var DEFAULT_RANGE = ['a', 'b', 'c', 'd', 'e', 'f'];
  var scale;
  beforeEach(function () {
    scale = d3scaleCluster();
  });

  it('should find clusters', function () {
    scale.domain(DEFAULT_DOMAIN).range(DEFAULT_RANGE);

    var clusters = scale.clusters();
    expect(clusters).toEqual([12, 43, 123, 234, 1244]);
    expect(scale(52)).toEqual('c');
    var exported = scale.export();
    var newScale = d3scaleCluster().import(exported);
    expect(newScale(52)).toEqual('c');
  });

  it('should be able to invert extent', function () {
    scale.domain(DEFAULT_DOMAIN).range(DEFAULT_RANGE);
    expect(scale.invertExtent('c')).toEqual([43, 123]); // Up to but not including 123
  });

  it('should return NaNs inverting an invalid value', function () {
    scale.domain(DEFAULT_DOMAIN).range(DEFAULT_RANGE);
    expect(scale.invertExtent('lol')).toEqual([NaN, NaN]);
  });

  it('should return undefined if no domain or range has been defined', function () {
    expect(scale(100)).toEqual(undefined);
  });

  it('should "gracefully" handle cases where range has more values than domain', function () {
    scale.domain([1, 2, 4]).range(DEFAULT_RANGE);
    expect(scale(4)).toEqual('c');
    var exported = scale.export();
    var newScale = d3scaleCluster().import(exported);
    expect(newScale(4)).toEqual('c');
  });
});
