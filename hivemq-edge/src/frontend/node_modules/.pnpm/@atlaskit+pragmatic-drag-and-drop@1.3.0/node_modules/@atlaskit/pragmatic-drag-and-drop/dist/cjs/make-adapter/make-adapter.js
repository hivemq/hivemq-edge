"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.makeAdapter = makeAdapter;
var _lifecycleManager = require("../ledger/lifecycle-manager");
var _usageLedger = require("../ledger/usage-ledger");
var _makeDropTarget = require("./make-drop-target");
var _makeMonitor = require("./make-monitor");
function makeAdapter(_ref) {
  var typeKey = _ref.typeKey,
    mount = _ref.mount,
    dispatchEventToSource = _ref.dispatchEventToSource,
    onPostDispatch = _ref.onPostDispatch,
    defaultDropEffect = _ref.defaultDropEffect;
  var monitorAPI = (0, _makeMonitor.makeMonitor)();
  var dropTargetAPI = (0, _makeDropTarget.makeDropTarget)({
    typeKey: typeKey,
    defaultDropEffect: defaultDropEffect
  });
  function dispatchEvent(args) {
    // 1. forward the event to source
    dispatchEventToSource === null || dispatchEventToSource === void 0 || dispatchEventToSource(args);

    // 2. forward the event to relevant dropTargets
    dropTargetAPI.dispatchEvent(args);

    // 3. forward event to monitors
    monitorAPI.dispatchEvent(args);

    // 4. post consumer dispatch (used for honey pot fix)
    onPostDispatch === null || onPostDispatch === void 0 || onPostDispatch(args);
  }
  function start(_ref2) {
    var event = _ref2.event,
      dragType = _ref2.dragType;
    _lifecycleManager.lifecycle.start({
      event: event,
      dragType: dragType,
      getDropTargetsOver: dropTargetAPI.getIsOver,
      dispatchEvent: dispatchEvent
    });
  }
  function registerUsage() {
    function mountAdapter() {
      var api = {
        canStart: _lifecycleManager.lifecycle.canStart,
        start: start
      };
      return mount(api);
    }
    return (0, _usageLedger.register)({
      typeKey: typeKey,
      mount: mountAdapter
    });
  }
  return {
    registerUsage: registerUsage,
    dropTarget: dropTargetAPI.dropTargetForConsumers,
    monitor: monitorAPI.monitorForConsumers
  };
}