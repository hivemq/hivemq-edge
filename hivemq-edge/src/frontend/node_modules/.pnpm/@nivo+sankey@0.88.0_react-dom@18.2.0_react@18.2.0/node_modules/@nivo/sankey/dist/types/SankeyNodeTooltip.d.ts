/// <reference types="react" />
import { DefaultLink, DefaultNode, SankeyNodeDatum } from './types';
export interface SankeyNodeTooltipProps<N extends DefaultNode, L extends DefaultLink> {
    node: SankeyNodeDatum<N, L>;
}
export declare const SankeyNodeTooltip: <N extends DefaultNode, L extends DefaultLink>({ node, }: SankeyNodeTooltipProps<N, L>) => JSX.Element;
//# sourceMappingURL=SankeyNodeTooltip.d.ts.map