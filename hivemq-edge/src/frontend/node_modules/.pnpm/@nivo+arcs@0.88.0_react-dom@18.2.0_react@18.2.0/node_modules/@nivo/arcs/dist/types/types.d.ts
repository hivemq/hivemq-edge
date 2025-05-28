import { Arc as D3Arc } from 'd3-shape';
export interface Point {
    x: number;
    y: number;
}
export interface Arc {
    startAngle: number;
    endAngle: number;
    innerRadius: number;
    outerRadius: number;
}
export interface DatumWithArc {
    id: string | number;
    arc: Arc;
}
export interface DatumWithArcAndColor extends DatumWithArc {
    color: string;
    fill?: string;
}
export type ArcGenerator = D3Arc<any, Arc>;
//# sourceMappingURL=types.d.ts.map