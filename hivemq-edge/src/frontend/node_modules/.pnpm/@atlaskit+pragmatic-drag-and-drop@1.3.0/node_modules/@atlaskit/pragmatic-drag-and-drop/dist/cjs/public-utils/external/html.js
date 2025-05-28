"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.containsHTML = containsHTML;
exports.getHTML = getHTML;
var _htmlMediaType = require("../../util/media-types/html-media-type");
function containsHTML(_ref) {
  var source = _ref.source;
  return source.types.includes(_htmlMediaType.HTMLMediaType);
}
function getHTML(_ref2) {
  var source = _ref2.source;
  return source.getStringData(_htmlMediaType.HTMLMediaType);
}