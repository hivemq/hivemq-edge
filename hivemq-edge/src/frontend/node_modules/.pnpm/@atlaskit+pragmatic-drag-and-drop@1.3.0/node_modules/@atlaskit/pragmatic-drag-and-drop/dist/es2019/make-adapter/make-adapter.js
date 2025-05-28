import { lifecycle } from '../ledger/lifecycle-manager';
import { register } from '../ledger/usage-ledger';
import { makeDropTarget } from './make-drop-target';
import { makeMonitor } from './make-monitor';
export function makeAdapter({
  typeKey,
  mount,
  dispatchEventToSource,
  onPostDispatch,
  defaultDropEffect
}) {
  const monitorAPI = makeMonitor();
  const dropTargetAPI = makeDropTarget({
    typeKey,
    defaultDropEffect
  });
  function dispatchEvent(args) {
    // 1. forward the event to source
    dispatchEventToSource === null || dispatchEventToSource === void 0 ? void 0 : dispatchEventToSource(args);

    // 2. forward the event to relevant dropTargets
    dropTargetAPI.dispatchEvent(args);

    // 3. forward event to monitors
    monitorAPI.dispatchEvent(args);

    // 4. post consumer dispatch (used for honey pot fix)
    onPostDispatch === null || onPostDispatch === void 0 ? void 0 : onPostDispatch(args);
  }
  function start({
    event,
    dragType
  }) {
    lifecycle.start({
      event,
      dragType,
      getDropTargetsOver: dropTargetAPI.getIsOver,
      dispatchEvent
    });
  }
  function registerUsage() {
    function mountAdapter() {
      const api = {
        canStart: lifecycle.canStart,
        start
      };
      return mount(api);
    }
    return register({
      typeKey,
      mount: mountAdapter
    });
  }
  return {
    registerUsage,
    dropTarget: dropTargetAPI.dropTargetForConsumers,
    monitor: monitorAPI.monitorForConsumers
  };
}