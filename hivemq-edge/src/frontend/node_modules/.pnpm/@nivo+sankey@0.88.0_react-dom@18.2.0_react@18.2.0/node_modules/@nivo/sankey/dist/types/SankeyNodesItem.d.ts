/// <reference types="react" />
import { DefaultLink, DefaultNode, SankeyCommonProps, SankeyNodeDatum } from './types';
interface SankeyNodesItemProps<N extends DefaultNode, L extends DefaultLink> {
    node: SankeyNodeDatum<N, L>;
    x: number;
    y: number;
    width: number;
    height: number;
    color: string;
    opacity: number;
    borderWidth: SankeyCommonProps<N, L>['nodeBorderWidth'];
    borderColor: string;
    borderRadius: SankeyCommonProps<N, L>['nodeBorderRadius'];
    setCurrent: (node: SankeyNodeDatum<N, L> | null) => void;
    isInteractive: SankeyCommonProps<N, L>['isInteractive'];
    onClick?: SankeyCommonProps<N, L>['onClick'];
    tooltip: SankeyCommonProps<N, L>['nodeTooltip'];
}
export declare const SankeyNodesItem: <N extends DefaultNode, L extends DefaultLink>({ node, x, y, width, height, color, opacity, borderWidth, borderColor, borderRadius, setCurrent, isInteractive, onClick, tooltip, }: SankeyNodesItemProps<N, L>) => JSX.Element;
export {};
//# sourceMappingURL=SankeyNodesItem.d.ts.map