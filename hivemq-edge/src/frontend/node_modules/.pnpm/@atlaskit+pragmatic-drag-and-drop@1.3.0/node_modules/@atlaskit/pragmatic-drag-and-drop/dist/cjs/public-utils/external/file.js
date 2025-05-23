"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.containsFiles = containsFiles;
exports.getFiles = getFiles;
function containsFiles(_ref) {
  var source = _ref.source;
  return source.types.includes('Files');
}

/** Obtain an array of the dragged `File`s */
function getFiles(_ref2) {
  var source = _ref2.source;
  return source.items
  // unlike other media types, for files:
  // item.kind is 'file'
  // item.type is the type of file eg 'image/jpg'
  // for other media types, item.type is the mime format
  .filter(function (item) {
    return item.kind === 'file';
  }).map(function (item) {
    return item.getAsFile();
  }).filter(function (file) {
    return file != null;
  });
}