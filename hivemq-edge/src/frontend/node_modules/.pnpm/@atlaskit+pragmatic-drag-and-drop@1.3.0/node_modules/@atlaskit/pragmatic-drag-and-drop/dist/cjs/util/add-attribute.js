"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.addAttribute = addAttribute;
function addAttribute(element, _ref) {
  var attribute = _ref.attribute,
    value = _ref.value;
  element.setAttribute(attribute, value);
  return function () {
    return element.removeAttribute(attribute);
  };
}