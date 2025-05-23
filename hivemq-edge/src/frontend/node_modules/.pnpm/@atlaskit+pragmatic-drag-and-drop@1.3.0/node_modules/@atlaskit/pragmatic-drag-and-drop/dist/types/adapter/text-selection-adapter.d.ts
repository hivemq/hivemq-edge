import { type BaseEventPayload, type CleanupFn, type DropTargetEventBasePayload, type DropTargetEventPayloadMap, type DropTargetGetFeedbackArgs, type EventPayloadMap, type MonitorGetFeedbackArgs, type TextSelectionDragType } from '../internal-types';
declare const adapter: {
    registerUsage: () => CleanupFn;
    dropTarget: (args: import("../internal-types").DropTargetArgs<TextSelectionDragType>) => CleanupFn;
    monitor: (args: import("../internal-types").MonitorArgs<TextSelectionDragType>) => CleanupFn;
};
type StripPreviewEvent<T> = Omit<T, 'onGenerateDragPreview'>;
export declare function dropTargetForTextSelection(args: StripPreviewEvent<Parameters<typeof adapter.dropTarget>[0]>): CleanupFn;
export declare function monitorForTextSelection(args: StripPreviewEvent<Parameters<typeof adapter.monitor>[0]>): CleanupFn;
/** Common event payload for all events */
export type TextSelectionEventBasePayload = BaseEventPayload<TextSelectionDragType>;
/** A map containing payloads for all events */
export type TextSelectionEventPayloadMap = StripPreviewEvent<EventPayloadMap<TextSelectionDragType>>;
/** Common event payload for all drop target events */
export type TextSelectionDropTargetEventBasePayload = DropTargetEventBasePayload<TextSelectionDragType>;
/** A map containing payloads for all events on drop targets */
export type TextSelectionDropTargetEventPayloadMap = StripPreviewEvent<DropTargetEventPayloadMap<TextSelectionDragType>>;
/** Argument given to all feedback functions (eg `canDrop()`) on a `dropTargetForExternal` */
export type TextSelectionMonitorGetFeedbackArgs = MonitorGetFeedbackArgs<TextSelectionDragType>;
/** Argument given to all monitor feedback functions (eg `canMonitor()`) for a `monitorForExternal` */
export type TextSelectionDropTargetGetFeedbackArgs = DropTargetGetFeedbackArgs<TextSelectionDragType>;
export {};
