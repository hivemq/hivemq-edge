"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.isLeavingWindow = isLeavingWindow;
var _isFirefox = require("../is-firefox");
var _isSafari = require("../is-safari");
var _countEventsForSafari = require("./count-events-for-safari");
var _isFromAnotherWindow = require("./is-from-another-window");
function isLeavingWindow(_ref) {
  var dragLeave = _ref.dragLeave;
  var type = dragLeave.type,
    relatedTarget = dragLeave.relatedTarget;
  if (type !== 'dragleave') {
    return false;
  }
  if ((0, _isSafari.isSafari)()) {
    return (0, _countEventsForSafari.isLeavingWindowInSafari)({
      dragLeave: dragLeave
    });
  }

  // Standard check: if going to `null` we are leaving the `window`
  if (relatedTarget == null) {
    return true;
  }

  /**
   * 🦊 Exception: `iframe` in Firefox (`125.0`)
   *
   * Case 1: parent `window` → child `iframe`
   * `dragLeave.relatedTarget` is element _inside_ the child `iframe`
   * (foreign element)
   *
   * Case 2: child `iframe` → parent `window`
   * `dragLeave.relatedTarget` is the `iframe` in the parent `window`
   * (foreign element)
   */

  if ((0, _isFirefox.isFirefox)()) {
    return (0, _isFromAnotherWindow.isFromAnotherWindow)(relatedTarget);
  }

  /**
   * 🌏 Exception: `iframe` in Chrome (`124.0`)
   *
   * Case 1: parent `window` → child `iframe`
   * `dragLeave.relatedTarget` is the `iframe` in the parent `window`
   *
   * Case 2: child `iframe` → parent `window`
   * `dragLeave.relatedTarget` is `null` *(standard check)*
   */

  // Case 2
  // Using `instanceof` check as the element will be in the same `window`
  return relatedTarget instanceof HTMLIFrameElement;
}