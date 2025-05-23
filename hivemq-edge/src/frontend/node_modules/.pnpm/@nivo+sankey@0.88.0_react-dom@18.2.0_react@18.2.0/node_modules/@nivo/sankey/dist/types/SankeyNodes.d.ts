/// <reference types="react" />
import { DefaultLink, DefaultNode, SankeyCommonProps, SankeyLinkDatum, SankeyNodeDatum } from './types';
interface SankeyNodesProps<N extends DefaultNode, L extends DefaultLink> {
    nodes: SankeyNodeDatum<N, L>[];
    nodeOpacity: SankeyCommonProps<N, L>['nodeOpacity'];
    nodeHoverOpacity: SankeyCommonProps<N, L>['nodeHoverOpacity'];
    nodeHoverOthersOpacity: SankeyCommonProps<N, L>['nodeHoverOthersOpacity'];
    borderWidth: SankeyCommonProps<N, L>['nodeBorderWidth'];
    getBorderColor: (node: SankeyNodeDatum<N, L>) => string;
    borderRadius: SankeyCommonProps<N, L>['nodeBorderRadius'];
    setCurrentNode: (node: SankeyNodeDatum<N, L> | null) => void;
    currentNode: SankeyNodeDatum<N, L> | null;
    currentLink: SankeyLinkDatum<N, L> | null;
    isCurrentNode: (node: SankeyNodeDatum<N, L>) => boolean;
    isInteractive: SankeyCommonProps<N, L>['isInteractive'];
    onClick?: SankeyCommonProps<N, L>['onClick'];
    tooltip: SankeyCommonProps<N, L>['nodeTooltip'];
}
export declare const SankeyNodes: <N extends DefaultNode, L extends DefaultLink>({ nodes, nodeOpacity, nodeHoverOpacity, nodeHoverOthersOpacity, borderWidth, getBorderColor, borderRadius, setCurrentNode, currentNode, currentLink, isCurrentNode, isInteractive, onClick, tooltip, }: SankeyNodesProps<N, L>) => JSX.Element;
export {};
//# sourceMappingURL=SankeyNodes.d.ts.map