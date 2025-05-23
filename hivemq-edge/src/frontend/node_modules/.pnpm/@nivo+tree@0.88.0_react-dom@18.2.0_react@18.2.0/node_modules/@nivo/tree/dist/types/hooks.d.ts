import { MouseEvent } from 'react';
import { Margin } from '@nivo/core';
import { TooltipAnchor, TooltipPosition } from '@nivo/tooltip';
import { DefaultDatum, HierarchyTreeNode, TreeDataProps, CommonProps, Layout, ComputedNode, ComputedLink, NodeMouseEventHandler, NodeTooltip, IntermediateComputedLink, LinkThicknessFunction, LinkMouseEventHandler, LinkTooltip, IntermediateComputedNode, CurrentNodeSetter, NodeSizeModifierFunction, LinkThicknessModifierFunction, TreeMode, LinkCurve } from './types';
export declare const useRoot: <Datum>({ data, mode, getIdentity, }: {
    data: Datum;
    mode: TreeMode;
    getIdentity: (node: Datum) => string;
}) => HierarchyTreeNode<Datum>;
export declare const useTree: <Datum = DefaultDatum>({ data, width, height, identity, mode, layout, nodeSize, activeNodeSize, inactiveNodeSize, nodeColor, fixNodeColorAtDepth, highlightAncestorNodes, highlightDescendantNodes, linkCurve, linkThickness, linkColor, activeLinkThickness, inactiveLinkThickness, highlightAncestorLinks, highlightDescendantLinks, }: {
    data: Datum;
    width: number;
    height: number;
    identity?: import("@nivo/core").PropertyAccessor<Datum, string> | undefined;
    mode?: TreeMode | undefined;
    layout?: Layout | undefined;
    nodeSize?: number | import("./types").NodeSizeFunction<Datum> | undefined;
    activeNodeSize?: number | NodeSizeModifierFunction<Datum> | undefined;
    inactiveNodeSize?: number | NodeSizeModifierFunction<Datum> | undefined;
    nodeColor?: import("@nivo/colors").OrdinalColorScaleConfig<IntermediateComputedNode<Datum>> | undefined;
    fixNodeColorAtDepth?: number | undefined;
    highlightAncestorNodes?: boolean | undefined;
    highlightDescendantNodes?: boolean | undefined;
    linkCurve?: LinkCurve | undefined;
    linkThickness?: number | LinkThicknessFunction<Datum> | undefined;
    activeLinkThickness?: number | LinkThicknessModifierFunction<Datum> | undefined;
    inactiveLinkThickness?: number | LinkThicknessModifierFunction<Datum> | undefined;
    linkColor?: import("@nivo/colors").InheritedColorConfig<IntermediateComputedLink<Datum>> | undefined;
    highlightAncestorLinks?: boolean | undefined;
    highlightDescendantLinks?: boolean | undefined;
}) => {
    nodes: ComputedNode<Datum>[];
    nodeByUid: Record<string, ComputedNode<Datum>>;
    links: ComputedLink<Datum>[];
    linkGenerator: import("d3-shape").Link<any, import("d3-shape").DefaultLinkObject, [number, number]>;
    setCurrentNode: (node: ComputedNode<Datum> | null) => void;
};
/**
 * This hook may generates mouse event handlers for a node according to the main chart props.
 * It's used for the default `Node` component and may be used for custom nodes
 * to simplify their implementation.
 */
export declare const useNodeMouseEventHandlers: <Datum>(node: ComputedNode<Datum>, { isInteractive, onMouseEnter, onMouseMove, onMouseLeave, onClick, setCurrentNode, tooltip, tooltipPosition, tooltipAnchor, margin, }: {
    isInteractive: boolean;
    onMouseEnter?: NodeMouseEventHandler<Datum> | undefined;
    onMouseMove?: NodeMouseEventHandler<Datum> | undefined;
    onMouseLeave?: NodeMouseEventHandler<Datum> | undefined;
    onClick?: NodeMouseEventHandler<Datum> | undefined;
    setCurrentNode: CurrentNodeSetter<Datum>;
    tooltip?: NodeTooltip<Datum> | undefined;
    tooltipPosition: TooltipPosition;
    tooltipAnchor: TooltipAnchor;
    margin: Margin;
}) => {
    onMouseEnter: ((event: MouseEvent) => void) | undefined;
    onMouseMove: ((event: MouseEvent) => void) | undefined;
    onMouseLeave: ((event: MouseEvent) => void) | undefined;
    onClick: ((event: MouseEvent) => void) | undefined;
};
/**
 * This hook may generates mouse event handlers for a node according to the main chart props.
 * It's used for the default `Node` component and may be used for custom nodes
 * to simplify their implementation.
 */
export declare const useLinkMouseEventHandlers: <Datum>(link: ComputedLink<Datum>, { isInteractive, onMouseEnter, onMouseMove, onMouseLeave, onClick, tooltip, tooltipAnchor, }: {
    isInteractive: boolean;
    onMouseEnter?: LinkMouseEventHandler<Datum> | undefined;
    onMouseMove?: LinkMouseEventHandler<Datum> | undefined;
    onMouseLeave?: LinkMouseEventHandler<Datum> | undefined;
    onClick?: LinkMouseEventHandler<Datum> | undefined;
    tooltip?: LinkTooltip<Datum> | undefined;
    tooltipAnchor: TooltipAnchor;
}) => {
    onMouseEnter: ((event: MouseEvent) => void) | undefined;
    onMouseMove: ((event: MouseEvent) => void) | undefined;
    onMouseLeave: ((event: MouseEvent) => void) | undefined;
    onClick: ((event: MouseEvent) => void) | undefined;
};
//# sourceMappingURL=hooks.d.ts.map