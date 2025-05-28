import { type AllDragTypes, type DragLocation, type EventPayloadMap } from '../internal-types';
export declare function makeDispatch<DragType extends AllDragTypes>({ source, initial, dispatchEvent, }: {
    source: DragType['payload'];
    initial: DragLocation;
    dispatchEvent: <EventName extends keyof EventPayloadMap<DragType>>(args: {
        eventName: EventName;
        payload: EventPayloadMap<DragType>[EventName];
    }) => void;
}): {
    start({ nativeSetDragImage }: {
        nativeSetDragImage: DataTransfer['setDragImage'] | null;
    }): void;
    dragUpdate({ current }: {
        current: DragLocation;
    }): void;
    drag({ current }: {
        current: DragLocation;
    }): void;
    drop({ current, updatedSourcePayload, }: {
        current: DragLocation;
        /** When dragging from an external source, we need to collect the
      drag source information again as it is often only available during
      the "drop" event */
        updatedSourcePayload: DragType['payload'] | null;
    }): void;
};
