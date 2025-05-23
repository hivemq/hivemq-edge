"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.centerUnderPointer = void 0;
var centerUnderPointer = exports.centerUnderPointer = function centerUnderPointer(_ref) {
  var container = _ref.container;
  var rect = container.getBoundingClientRect();
  return {
    x: rect.width / 2,
    y: rect.height / 2
  };
};