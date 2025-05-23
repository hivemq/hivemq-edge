import { MouseEvent, TouchEvent } from 'react';
import { TooltipAnchor } from './types';
export interface TooltipActionsContextData {
    showTooltipAt: (content: JSX.Element, position: [number, number], anchor?: TooltipAnchor) => void;
    showTooltipFromEvent: (content: JSX.Element, event: MouseEvent | TouchEvent, anchor?: TooltipAnchor) => void;
    hideTooltip: () => void;
}
export declare const TooltipActionsContext: import("react").Context<TooltipActionsContextData>;
export interface TooltipStateContextDataVisible {
    isVisible: true;
    position: [number, number];
    content: JSX.Element;
    anchor: TooltipAnchor;
}
export interface TooltipStateContextDataHidden {
    isVisible: false;
    position: [null, null];
    content: null;
    anchor: null;
}
export type TooltipStateContextData = TooltipStateContextDataVisible | TooltipStateContextDataHidden;
export declare const hiddenTooltipState: TooltipStateContextDataHidden;
export declare const TooltipStateContext: import("react").Context<TooltipStateContextData>;
//# sourceMappingURL=context.d.ts.map