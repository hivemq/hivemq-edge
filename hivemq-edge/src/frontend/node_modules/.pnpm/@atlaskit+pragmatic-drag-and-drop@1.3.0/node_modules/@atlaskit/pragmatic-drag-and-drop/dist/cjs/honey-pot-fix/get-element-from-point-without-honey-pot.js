"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");
Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.getElementFromPointWithoutHoneypot = getElementFromPointWithoutHoneypot;
var _slicedToArray2 = _interopRequireDefault(require("@babel/runtime/helpers/slicedToArray"));
var _isHoneyPotElement = require("./is-honey-pot-element");
function getElementFromPointWithoutHoneypot(client) {
  // eslint-disable-next-line no-restricted-syntax
  var _document$elementsFro = document.elementsFromPoint(client.x, client.y),
    _document$elementsFro2 = (0, _slicedToArray2.default)(_document$elementsFro, 2),
    top = _document$elementsFro2[0],
    second = _document$elementsFro2[1];
  if (!top) {
    return null;
  }
  if ((0, _isHoneyPotElement.isHoneyPotElement)(top)) {
    return second !== null && second !== void 0 ? second : null;
  }
  return top;
}