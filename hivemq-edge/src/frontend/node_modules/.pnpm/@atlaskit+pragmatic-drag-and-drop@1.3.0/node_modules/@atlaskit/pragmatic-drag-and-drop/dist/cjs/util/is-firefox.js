"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.isFirefox = void 0;
var _once = require("../public-utils/once");
// using `cache` as our `isFirefox()` result will not change in a browser

/**
 * Returns `true` if a `Firefox` browser
 * */
var isFirefox = exports.isFirefox = (0, _once.once)(function isFirefox() {
  if (process.env.NODE_ENV === 'test') {
    return false;
  }
  return navigator.userAgent.includes('Firefox');
});