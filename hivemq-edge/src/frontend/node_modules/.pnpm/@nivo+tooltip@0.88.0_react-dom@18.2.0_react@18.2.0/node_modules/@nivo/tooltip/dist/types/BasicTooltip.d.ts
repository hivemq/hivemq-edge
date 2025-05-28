import { ReactNode } from 'react';
import { ValueFormat } from '@nivo/core';
export interface BasicTooltipProps {
    id: ReactNode;
    value?: number | string | Date;
    format?: ValueFormat<number | string | Date>;
    color?: string;
    enableChip?: boolean;
    /**
     * @deprecated This should be replaced by custom tooltip components.
     */
    renderContent?: () => JSX.Element;
}
export declare const BasicTooltip: import("react").NamedExoticComponent<BasicTooltipProps>;
//# sourceMappingURL=BasicTooltip.d.ts.map