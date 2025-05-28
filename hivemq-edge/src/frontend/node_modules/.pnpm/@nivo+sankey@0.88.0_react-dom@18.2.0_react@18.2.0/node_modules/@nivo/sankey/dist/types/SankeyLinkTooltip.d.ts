/// <reference types="react" />
import { DefaultLink, DefaultNode, SankeyLinkDatum } from './types';
export interface SankeyLinkTooltipProps<N extends DefaultNode, L extends DefaultLink> {
    link: SankeyLinkDatum<N, L>;
}
export declare const SankeyLinkTooltip: <N extends DefaultNode, L extends DefaultLink>({ link, }: SankeyLinkTooltipProps<N, L>) => JSX.Element;
//# sourceMappingURL=SankeyLinkTooltip.d.ts.map