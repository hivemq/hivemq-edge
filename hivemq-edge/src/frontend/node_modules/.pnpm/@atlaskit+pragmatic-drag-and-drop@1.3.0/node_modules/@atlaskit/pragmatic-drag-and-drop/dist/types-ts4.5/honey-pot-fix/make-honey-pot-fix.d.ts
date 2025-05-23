import { type AllDragTypes, type BaseEventPayload, type CleanupFn, type EventPayloadMap } from '../internal-types';
export declare function makeHoneyPotFix(): {
    bindEvents: () => CleanupFn;
    getOnPostDispatch: () => ({ eventName, payload, }: {
        eventName: keyof EventPayloadMap<AllDragTypes>;
        payload: BaseEventPayload<AllDragTypes>;
    }) => void;
};
