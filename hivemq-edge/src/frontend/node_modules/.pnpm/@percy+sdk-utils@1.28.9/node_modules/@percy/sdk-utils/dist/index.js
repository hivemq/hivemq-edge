"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
Object.defineProperty(exports, "captureAutomateScreenshot", {
  enumerable: true,
  get: function () {
    return _postScreenshot.default;
  }
});
exports.default = void 0;
Object.defineProperty(exports, "fetchPercyDOM", {
  enumerable: true,
  get: function () {
    return _percyDom.default;
  }
});
Object.defineProperty(exports, "flushSnapshots", {
  enumerable: true,
  get: function () {
    return _flushSnapshots.default;
  }
});
Object.defineProperty(exports, "isPercyEnabled", {
  enumerable: true,
  get: function () {
    return _percyEnabled.default;
  }
});
Object.defineProperty(exports, "logger", {
  enumerable: true,
  get: function () {
    return _logger.default;
  }
});
Object.defineProperty(exports, "percy", {
  enumerable: true,
  get: function () {
    return _percyInfo.default;
  }
});
Object.defineProperty(exports, "postBuildEvents", {
  enumerable: true,
  get: function () {
    return _postBuildEvent.default;
  }
});
Object.defineProperty(exports, "postComparison", {
  enumerable: true,
  get: function () {
    return _postComparison.default;
  }
});
Object.defineProperty(exports, "postSnapshot", {
  enumerable: true,
  get: function () {
    return _postSnapshot.default;
  }
});
Object.defineProperty(exports, "request", {
  enumerable: true,
  get: function () {
    return _request.default;
  }
});
Object.defineProperty(exports, "waitForPercyIdle", {
  enumerable: true,
  get: function () {
    return _percyIdle.default;
  }
});
var _logger = _interopRequireDefault(require("./logger.js"));
var _percyInfo = _interopRequireDefault(require("./percy-info.js"));
var _request = _interopRequireDefault(require("./request.js"));
var _percyEnabled = _interopRequireDefault(require("./percy-enabled.js"));
var _percyIdle = _interopRequireDefault(require("./percy-idle.js"));
var _percyDom = _interopRequireDefault(require("./percy-dom.js"));
var _postSnapshot = _interopRequireDefault(require("./post-snapshot.js"));
var _postComparison = _interopRequireDefault(require("./post-comparison.js"));
var _postBuildEvent = _interopRequireDefault(require("./post-build-event.js"));
var _flushSnapshots = _interopRequireDefault(require("./flush-snapshots.js"));
var _postScreenshot = _interopRequireDefault(require("./post-screenshot.js"));
var _default = _interopRequireWildcard(require("./index.js"));
exports.default = _default;
function _getRequireWildcardCache(e) { if ("function" != typeof WeakMap) return null; var r = new WeakMap(), t = new WeakMap(); return (_getRequireWildcardCache = function (e) { return e ? t : r; })(e); }
function _interopRequireWildcard(e, r) { if (!r && e && e.__esModule) return e; if (null === e || "object" != typeof e && "function" != typeof e) return { default: e }; var t = _getRequireWildcardCache(r); if (t && t.has(e)) return t.get(e); var n = { __proto__: null }, a = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var u in e) if ("default" !== u && {}.hasOwnProperty.call(e, u)) { var i = a ? Object.getOwnPropertyDescriptor(e, u) : null; i && (i.get || i.set) ? Object.defineProperty(n, u, i) : n[u] = e[u]; } return n.default = e, t && t.set(e, n), n; }
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }