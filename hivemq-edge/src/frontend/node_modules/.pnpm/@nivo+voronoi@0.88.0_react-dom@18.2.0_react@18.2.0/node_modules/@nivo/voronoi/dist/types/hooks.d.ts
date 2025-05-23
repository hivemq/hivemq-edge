import { MouseEvent, MutableRefObject, TouchEvent } from 'react';
import { Delaunay } from 'd3-delaunay';
import { Margin } from '@nivo/core';
import { TooltipAnchor, TooltipPosition } from '@nivo/tooltip';
import { VoronoiCommonProps, VoronoiDatum, VoronoiCustomLayerProps, NodeMouseHandler, NodePositionAccessor, NodeTouchHandler } from './types';
export declare const useVoronoiMesh: <Node_1>({ points, getNodePosition, width, height, margin, debug, }: {
    points: readonly Node_1[];
    getNodePosition?: NodePositionAccessor<Node_1> | undefined;
    margin?: Margin | undefined;
    width: number;
    height: number;
    debug?: boolean | undefined;
}) => {
    points: readonly [number, number][];
    delaunay: Delaunay<Delaunay.Point>;
    voronoi: import("d3-delaunay").Voronoi<Delaunay.Point> | undefined;
};
export declare const useVoronoi: ({ data, width, height, xDomain, yDomain, }: {
    data: VoronoiDatum[];
    width: number;
    height: number;
    xDomain: VoronoiCommonProps['xDomain'];
    yDomain: VoronoiCommonProps['yDomain'];
}) => {
    points: {
        x: number;
        y: number;
        data: VoronoiDatum;
    }[];
    delaunay: Delaunay<Delaunay.Point>;
    voronoi: import("d3-delaunay").Voronoi<Delaunay.Point>;
};
/**
 * Memoize the context to pass to custom layers.
 */
export declare const useVoronoiLayerContext: ({ points, delaunay, voronoi, }: VoronoiCustomLayerProps) => VoronoiCustomLayerProps;
export declare const useMeshEvents: <Node_1, ElementType extends Element>({ elementRef, nodes, getNodePosition, delaunay, setCurrent: setCurrentNode, margin, detectionRadius, isInteractive, onMouseEnter, onMouseMove, onMouseLeave, onClick, onTouchStart, onTouchMove, onTouchEnd, enableTouchCrosshair, tooltip, tooltipPosition, tooltipAnchor, }: {
    elementRef: MutableRefObject<ElementType | null>;
    nodes: readonly Node_1[];
    getNodePosition?: NodePositionAccessor<Node_1> | undefined;
    delaunay: Delaunay<Node_1>;
    setCurrent?: ((node: Node_1 | null) => void) | undefined;
    margin?: Margin | undefined;
    detectionRadius?: number | undefined;
    isInteractive?: boolean | undefined;
    onMouseEnter?: NodeMouseHandler<Node_1> | undefined;
    onMouseMove?: NodeMouseHandler<Node_1> | undefined;
    onMouseLeave?: NodeMouseHandler<Node_1> | undefined;
    onClick?: NodeMouseHandler<Node_1> | undefined;
    onTouchStart?: NodeTouchHandler<Node_1> | undefined;
    onTouchMove?: NodeTouchHandler<Node_1> | undefined;
    onTouchEnd?: NodeTouchHandler<Node_1> | undefined;
    enableTouchCrosshair?: boolean | undefined;
    tooltip?: ((node: Node_1) => JSX.Element) | undefined;
    tooltipPosition?: TooltipPosition | undefined;
    tooltipAnchor?: TooltipAnchor | undefined;
}) => {
    current: [number, Node_1] | null;
    handleMouseEnter: ((event: MouseEvent<ElementType, globalThis.MouseEvent>) => void) | undefined;
    handleMouseMove: ((event: MouseEvent<ElementType, globalThis.MouseEvent>) => void) | undefined;
    handleMouseLeave: ((event: MouseEvent<ElementType, globalThis.MouseEvent>) => void) | undefined;
    handleClick: ((event: MouseEvent<ElementType, globalThis.MouseEvent>) => void) | undefined;
    handleTouchStart: ((event: TouchEvent<ElementType>) => void) | undefined;
    handleTouchMove: ((event: TouchEvent<ElementType>) => void) | undefined;
    handleTouchEnd: ((event: TouchEvent<SVGRectElement>) => void) | undefined;
};
/**
 * Compute a voronoi mesh and corresponding events.
 */
export declare const useMesh: <Node_1, ElementType extends Element>({ elementRef, nodes, getNodePosition, width, height, margin, isInteractive, detectionRadius, setCurrent, onMouseEnter, onMouseMove, onMouseLeave, onClick, tooltip, tooltipPosition, tooltipAnchor, debug, }: {
    elementRef: MutableRefObject<ElementType | null>;
    nodes: readonly Node_1[];
    getNodePosition?: NodePositionAccessor<Node_1> | undefined;
    width: number;
    height: number;
    margin?: Margin | undefined;
    isInteractive?: boolean | undefined;
    detectionRadius?: number | undefined;
    setCurrent?: ((node: Node_1 | null) => void) | undefined;
    onMouseEnter?: NodeMouseHandler<Node_1> | undefined;
    onMouseMove?: NodeMouseHandler<Node_1> | undefined;
    onMouseLeave?: NodeMouseHandler<Node_1> | undefined;
    onClick?: NodeMouseHandler<Node_1> | undefined;
    tooltip?: ((node: Node_1) => JSX.Element) | undefined;
    tooltipPosition?: TooltipPosition | undefined;
    tooltipAnchor?: TooltipAnchor | undefined;
    debug?: boolean | undefined;
}) => {
    delaunay: Delaunay<Delaunay.Point>;
    voronoi: import("d3-delaunay").Voronoi<Delaunay.Point> | undefined;
    current: [number, Node_1] | null;
    handleMouseEnter: ((event: MouseEvent<ElementType, globalThis.MouseEvent>) => void) | undefined;
    handleMouseMove: ((event: MouseEvent<ElementType, globalThis.MouseEvent>) => void) | undefined;
    handleMouseLeave: ((event: MouseEvent<ElementType, globalThis.MouseEvent>) => void) | undefined;
    handleClick: ((event: MouseEvent<ElementType, globalThis.MouseEvent>) => void) | undefined;
};
//# sourceMappingURL=hooks.d.ts.map