import { DefaultLink, DefaultNode, SankeyLinkDatum } from './types';
export declare const sankeyLinkHorizontal: <N extends DefaultNode, L extends DefaultLink>() => (link: SankeyLinkDatum<N, L>, contract: number) => string;
export declare const sankeyLinkVertical: <N extends DefaultNode, L extends DefaultLink>() => (link: SankeyLinkDatum<N, L>, contract: number) => string;
//# sourceMappingURL=links.d.ts.map