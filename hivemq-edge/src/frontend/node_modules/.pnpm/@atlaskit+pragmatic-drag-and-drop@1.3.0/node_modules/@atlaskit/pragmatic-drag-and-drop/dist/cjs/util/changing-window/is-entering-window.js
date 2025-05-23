"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.isEnteringWindow = isEnteringWindow;
var _isFirefox = require("../is-firefox");
var _isSafari = require("../is-safari");
var _countEventsForSafari = require("./count-events-for-safari");
var _isFromAnotherWindow = require("./is-from-another-window");
function isEnteringWindow(_ref) {
  var dragEnter = _ref.dragEnter;
  var type = dragEnter.type,
    relatedTarget = dragEnter.relatedTarget;
  if (type !== 'dragenter') {
    return false;
  }
  if ((0, _isSafari.isSafari)()) {
    return (0, _countEventsForSafari.isEnteringWindowInSafari)({
      dragEnter: dragEnter
    });
  }

  // standard check
  if (relatedTarget == null) {
    return true;
  }

  /**
   * 🦊 Exception: `iframe` in Firefox (`125.0`)
   *
   * Case 1: parent `window` → child `iframe`
   * `relatedTarget` is the `iframe` element in the parent `window`
   * (foreign element)
   *
   * Case 2: child `iframe` → parent `window`
   * `relatedTarget` is an element inside the child `iframe`
   * (foreign element)
   */

  if ((0, _isFirefox.isFirefox)()) {
    return (0, _isFromAnotherWindow.isFromAnotherWindow)(relatedTarget);
  }

  /**
   * 🌏 Exception: `iframe` in Chrome (`124.0`)
   *
   * Case 1: parent `window` → child `iframe`
   * `relatedTarget` is `null` *(standard check)*
   *
   * Case 2: child `iframe` → parent `window`
   * `relatedTarget` is the `iframe` element in the parent `window`
   */

  // Case 2
  // Using `instanceof` check as the element will be in the same `window`
  return relatedTarget instanceof HTMLIFrameElement;
}