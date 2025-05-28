"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.isHoneyPotElement = isHoneyPotElement;
var _honeyPotDataAttribute = require("./honey-pot-data-attribute");
function isHoneyPotElement(target) {
  return target instanceof Element && target.hasAttribute(_honeyPotDataAttribute.honeyPotDataAttribute);
}