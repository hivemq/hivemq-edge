"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.URLMediaType = void 0;
// Why we put the media types in their own files:
//
// - we are not putting them all in one file as not all adapters need all types
// - we are not putting them in the external helpers as some things just need the
//   types and not the external functions code
var URLMediaType = exports.URLMediaType = 'text/uri-list';