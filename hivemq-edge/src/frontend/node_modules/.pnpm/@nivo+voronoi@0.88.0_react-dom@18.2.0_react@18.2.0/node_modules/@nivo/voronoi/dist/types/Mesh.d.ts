/// <reference types="react" />
import { Margin } from '@nivo/core';
import { TooltipAnchor, TooltipPosition } from '@nivo/tooltip';
import { NodeMouseHandler, NodePositionAccessor, NodeTouchHandler } from './types';
interface MeshProps<Node> {
    nodes: Node[];
    width: number;
    height: number;
    margin?: Margin;
    getNodePosition?: NodePositionAccessor<Node>;
    setCurrent?: (node: Node | null) => void;
    onMouseEnter?: NodeMouseHandler<Node>;
    onMouseMove?: NodeMouseHandler<Node>;
    onMouseLeave?: NodeMouseHandler<Node>;
    onClick?: NodeMouseHandler<Node>;
    onTouchStart?: NodeTouchHandler<Node>;
    onTouchMove?: NodeTouchHandler<Node>;
    onTouchEnd?: NodeTouchHandler<Node>;
    enableTouchCrosshair?: boolean;
    detectionRadius?: number;
    tooltip?: (node: Node) => JSX.Element;
    tooltipPosition?: TooltipPosition;
    tooltipAnchor?: TooltipAnchor;
    debug?: boolean;
}
export declare const Mesh: <Node_1>({ nodes, width, height, margin, getNodePosition, setCurrent, onMouseEnter, onMouseMove, onMouseLeave, onClick, onTouchStart, onTouchMove, onTouchEnd, enableTouchCrosshair, detectionRadius, tooltip, tooltipPosition, tooltipAnchor, debug, }: MeshProps<Node_1>) => JSX.Element;
export {};
//# sourceMappingURL=Mesh.d.ts.map