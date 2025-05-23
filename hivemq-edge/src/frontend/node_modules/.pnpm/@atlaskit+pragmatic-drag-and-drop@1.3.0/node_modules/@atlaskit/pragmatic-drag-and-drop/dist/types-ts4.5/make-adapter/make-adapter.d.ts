import { type AdapterAPI, type AllDragTypes, type CleanupFn, type DropTargetAllowedDropEffect, type EventPayloadMap } from '../internal-types';
export declare function makeAdapter<DragType extends AllDragTypes>({ typeKey, mount, dispatchEventToSource, onPostDispatch, defaultDropEffect, }: {
    typeKey: DragType['type'];
    mount: (api: AdapterAPI<DragType>) => CleanupFn;
    defaultDropEffect: DropTargetAllowedDropEffect;
    dispatchEventToSource?: <EventName extends keyof EventPayloadMap<DragType>>(args: {
        eventName: EventName;
        payload: EventPayloadMap<DragType>[EventName];
    }) => void;
    onPostDispatch?: <EventName extends keyof EventPayloadMap<DragType>>(args: {
        eventName: EventName;
        payload: EventPayloadMap<DragType>[EventName];
    }) => void;
}): {
    registerUsage: () => CleanupFn;
    dropTarget: (args: import("../internal-types").DropTargetArgs<DragType>) => CleanupFn;
    monitor: (args: import("../internal-types").MonitorArgs<DragType>) => CleanupFn;
};
