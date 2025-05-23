import rafSchd from 'raf-schd';
const scheduleOnDrag = rafSchd(fn => fn());
const dragStart = (() => {
  let scheduled = null;
  function schedule(fn) {
    const frameId = requestAnimationFrame(() => {
      scheduled = null;
      fn();
    });
    scheduled = {
      frameId: frameId,
      fn
    };
  }
  function flush() {
    if (scheduled) {
      cancelAnimationFrame(scheduled.frameId);
      scheduled.fn();
      scheduled = null;
    }
  }
  return {
    schedule,
    flush
  };
})();
export function makeDispatch({
  source,
  initial,
  dispatchEvent
}) {
  let previous = {
    dropTargets: []
  };
  function safeDispatch(args) {
    dispatchEvent(args);
    previous = {
      dropTargets: args.payload.location.current.dropTargets
    };
  }
  const dispatch = {
    start({
      nativeSetDragImage
    }) {
      // Ensuring that both `onGenerateDragPreview` and `onDragStart` get the same location.
      // We do this so that `previous` is`[]` in `onDragStart` (which is logical)
      const location = {
        current: initial,
        previous,
        initial
      };
      // a `onGenerateDragPreview` does _not_ add another entry for `previous`
      // onDragPreview
      safeDispatch({
        eventName: 'onGenerateDragPreview',
        payload: {
          source,
          location,
          nativeSetDragImage
        }
      });
      dragStart.schedule(() => {
        safeDispatch({
          eventName: 'onDragStart',
          payload: {
            source,
            location
          }
        });
      });
    },
    dragUpdate({
      current
    }) {
      dragStart.flush();
      scheduleOnDrag.cancel();
      safeDispatch({
        eventName: 'onDropTargetChange',
        payload: {
          source,
          location: {
            initial,
            previous,
            current
          }
        }
      });
    },
    drag({
      current
    }) {
      scheduleOnDrag(() => {
        dragStart.flush();
        const location = {
          initial,
          previous,
          current
        };
        safeDispatch({
          eventName: 'onDrag',
          payload: {
            source,
            location
          }
        });
      });
    },
    drop({
      current,
      updatedSourcePayload
    }) {
      dragStart.flush();
      scheduleOnDrag.cancel();
      safeDispatch({
        eventName: 'onDrop',
        payload: {
          source: updatedSourcePayload !== null && updatedSourcePayload !== void 0 ? updatedSourcePayload : source,
          location: {
            current,
            previous,
            initial
          }
        }
      });
    }
  };
  return dispatch;
}