import { type AllDragTypes, type CleanupFn, type EventPayloadMap, type MonitorArgs } from '../internal-types';
export declare function makeMonitor<DragType extends AllDragTypes>(): {
    dispatchEvent: <EventName extends keyof EventPayloadMap<DragType>>({ eventName, payload, }: {
        eventName: EventName;
        payload: EventPayloadMap<DragType>[EventName];
    }) => void;
    monitorForConsumers: (args: MonitorArgs<DragType>) => CleanupFn;
};
