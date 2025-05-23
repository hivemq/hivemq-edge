"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.containsText = containsText;
exports.getText = getText;
var _textMediaType = require("../../util/media-types/text-media-type");
function containsText(_ref) {
  var source = _ref.source;
  return source.types.includes(_textMediaType.textMediaType);
}

/* Get the plain text that a user is dragging */
function getText(_ref2) {
  var source = _ref2.source;
  return source.getStringData(_textMediaType.textMediaType);
}