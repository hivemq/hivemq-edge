"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.containsURLs = containsURLs;
exports.getURLs = getURLs;
var _urlMediaType = require("../../util/media-types/url-media-type");
/**
 * 🦊🐞
 * When dragging a URL from the Firefox address bar or bookmarks
 * they are currently not adding an entry for "text/uri-list".
 * They add "text/x-moz-url" data which contains the same information
 * in a different format.
 *
 * [Bug report](https://bugzilla.mozilla.org/show_bug.cgi?id=1912164)
 */
var firefoxURLType = 'text/x-moz-url';
function containsURLs(_ref) {
  var source = _ref.source;
  return source.types.includes(_urlMediaType.URLMediaType) || source.types.includes(firefoxURLType);
}
function getURLs(_ref2) {
  var source = _ref2.source;
  var standard = source.getStringData(_urlMediaType.URLMediaType);
  if (standard != null) {
    return standard
    // You can have multiple urls split by CR+LF (EOL)
    // - CR: Carriage Return '\r'
    // - LF: Line Feed '\n'
    // - EOL: End of Line '\r\n'
    .split('\r\n')
    // a uri-list can have comment lines starting with '#'
    // so we need to remove those
    .filter(function (piece) {
      return !piece.startsWith('#');
    });
  }
  var fallback = source.getStringData(firefoxURLType);
  if (fallback != null) {
    return fallback
    // Values are split by a single LF: Line Feed (`\n`) character.
    // It's not clear from the "text/x-moz-url" documentation that
    // it's use `\n` and not `\r\n`, but based on testing and some
    // Github code searches it seems like `\n` is correct.
    // https://developer.mozilla.org/en-US/docs/Web/API/HTML_Drag_and_Drop_API/Recommended_drag_types#dragging_links
    .split('\n')
    // Every second line is the title of the url previous url.
    // We are ignoring the page titles in this helper
    .filter(function (_, index) {
      return index % 2 === 0;
    });
  }
  return [];
}