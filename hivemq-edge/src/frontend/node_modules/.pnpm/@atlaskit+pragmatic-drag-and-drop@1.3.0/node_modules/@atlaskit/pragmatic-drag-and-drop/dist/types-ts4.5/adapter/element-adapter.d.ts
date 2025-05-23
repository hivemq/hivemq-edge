import { type AllEvents, type BaseEventPayload, type CleanupFn, type DropTargetEventBasePayload, type DropTargetEventPayloadMap, type DropTargetGetFeedbackArgs, type ElementDragType, type EventPayloadMap, type Input, type MonitorGetFeedbackArgs, type NativeMediaType } from '../internal-types';
type DraggableGetFeedbackArgs = {
    /**
     * The user input as a drag is trying to start (the `initial` input)
     */
    input: Input;
    /**
     * The `draggable` element
     */
    element: HTMLElement;
    /**
     * The `dragHandle` element for the `draggable`
     */
    dragHandle: Element | null;
};
type DraggableArgs = {
    /** The `HTMLElement` that you want to attach draggable behaviour to.
     * `element` is our unique _key_ for a draggable.
     * `element` is a `HTMLElement` as only a `HTMLElement`
     * can have a "draggable" attribute
     */
    element: HTMLElement;
    /** The part of a draggable `element` that you want to use to control the dragging of the whole `element` */
    dragHandle?: Element;
    /** Conditionally allow a drag to occur */
    canDrag?: (args: DraggableGetFeedbackArgs) => boolean;
    /** Used to attach data to a drag operation. Called once just before the drag starts */
    getInitialData?: (args: DraggableGetFeedbackArgs) => Record<string, unknown>;
    /** Attach data to the native drag data store.
     * This function is useful to attach native data that can be extracted by other web pages
     * or web applications
     * Attaching native data in this way will _not_ cause the native adapter on this page to start
     * Although it can cause a native adapter in other applications to start
     * @example getInitialDataForExternal(() => ({'text/plain': item.description}))
     * */
    getInitialDataForExternal?: (args: DraggableGetFeedbackArgs) => {
        [Key in NativeMediaType]?: string;
    };
} & Partial<AllEvents<ElementDragType>>;
export declare const dropTargetForElements: (args: import("../internal-types").DropTargetArgs<ElementDragType>) => CleanupFn;
export declare const monitorForElements: (args: import("../internal-types").MonitorArgs<ElementDragType>) => CleanupFn;
export declare function draggable(args: DraggableArgs): CleanupFn;
/** Common event payload for all events */
export type ElementEventBasePayload = BaseEventPayload<ElementDragType>;
/** A map containing payloads for all events */
export type ElementEventPayloadMap = EventPayloadMap<ElementDragType>;
/** Common event payload for all drop target events */
export type ElementDropTargetEventBasePayload = DropTargetEventBasePayload<ElementDragType>;
/** A map containing payloads for all events on drop targets */
export type ElementDropTargetEventPayloadMap = DropTargetEventPayloadMap<ElementDragType>;
/** Arguments given to all feedback functions (eg `canDrag()`) on for a `draggable()` */
export type ElementGetFeedbackArgs = DraggableGetFeedbackArgs;
/** Arguments given to all feedback functions (eg `canDrop()`) on a `dropTargetForElements()` */
export type ElementDropTargetGetFeedbackArgs = DropTargetGetFeedbackArgs<ElementDragType>;
/** Arguments given to all monitor feedback functions (eg `canMonitor()`) for a `monitorForElements` */
export type ElementMonitorGetFeedbackArgs = MonitorGetFeedbackArgs<ElementDragType>;
export {};
