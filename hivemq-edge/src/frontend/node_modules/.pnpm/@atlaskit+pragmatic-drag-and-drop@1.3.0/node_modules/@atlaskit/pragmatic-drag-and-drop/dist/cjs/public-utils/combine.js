"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.combine = combine;
/** Create a new combined function that will call all the provided functions */
function combine() {
  for (var _len = arguments.length, fns = new Array(_len), _key = 0; _key < _len; _key++) {
    fns[_key] = arguments[_key];
  }
  return function cleanup() {
    fns.forEach(function (fn) {
      return fn();
    });
  };
}