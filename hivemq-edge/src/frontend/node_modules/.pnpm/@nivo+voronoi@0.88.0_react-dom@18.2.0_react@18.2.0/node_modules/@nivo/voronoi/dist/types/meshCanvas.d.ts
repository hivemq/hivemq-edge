import { Delaunay, Voronoi } from 'd3-delaunay';
export declare const renderVoronoiToCanvas: (ctx: CanvasRenderingContext2D, voronoi: Voronoi<Delaunay.Point>) => void;
export declare const renderDelaunayPointsToCanvas: (ctx: CanvasRenderingContext2D, delaunay: Delaunay<Delaunay.Point>, radius: number) => void;
export declare const renderVoronoiCellToCanvas: (ctx: CanvasRenderingContext2D, voronoi: Voronoi<Delaunay.Point>, index: number) => void;
export declare const renderDebugToCanvas: (ctx: CanvasRenderingContext2D, { delaunay, voronoi, detectionRadius, index, }: {
    delaunay: Delaunay<Delaunay.Point>;
    voronoi: Voronoi<Delaunay.Point>;
    detectionRadius: number;
    index: number | null;
}) => void;
//# sourceMappingURL=meshCanvas.d.ts.map