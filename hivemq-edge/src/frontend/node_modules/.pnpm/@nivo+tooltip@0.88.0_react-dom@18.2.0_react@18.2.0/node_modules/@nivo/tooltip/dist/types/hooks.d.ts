import { MutableRefObject } from 'react';
import { TooltipActionsContextData, TooltipStateContextData } from './context';
export declare const useTooltipHandlers: (container: MutableRefObject<HTMLDivElement>) => {
    actions: TooltipActionsContextData;
    state: TooltipStateContextData;
};
export declare const useTooltip: () => TooltipActionsContextData;
export declare const useTooltipState: () => TooltipStateContextData;
//# sourceMappingURL=hooks.d.ts.map