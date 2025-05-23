import { type AllDragTypes, type DropTargetAPI, type EventPayloadMap } from '../internal-types';
declare function canStart(): boolean;
declare function start<DragType extends AllDragTypes>({ event, dragType, getDropTargetsOver, dispatchEvent, }: {
    event: DragEvent;
    dragType: DragType;
    getDropTargetsOver: DropTargetAPI<DragType>['getIsOver'];
    dispatchEvent: <EventName extends keyof EventPayloadMap<DragType>>(args: {
        eventName: EventName;
        payload: EventPayloadMap<DragType>[EventName];
    }) => void;
}): void;
export declare const lifecycle: {
    canStart: typeof canStart;
    start: typeof start;
};
export {};
