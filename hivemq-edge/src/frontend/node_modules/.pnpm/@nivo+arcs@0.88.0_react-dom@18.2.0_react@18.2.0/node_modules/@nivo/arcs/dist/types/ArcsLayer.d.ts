/// <reference types="react" />
import { InheritedColorConfig } from '@nivo/colors';
import { DatumWithArcAndColor, ArcGenerator } from './types';
import { ArcTransitionMode } from './arcTransitionMode';
import { ArcMouseHandler, ArcShapeProps } from './ArcShape';
export type ArcComponent<Datum extends DatumWithArcAndColor> = (props: ArcShapeProps<Datum>) => JSX.Element;
interface ArcsLayerProps<Datum extends DatumWithArcAndColor> {
    center: [number, number];
    data: Datum[];
    arcGenerator: ArcGenerator;
    borderWidth: number;
    borderColor: InheritedColorConfig<Datum>;
    onClick?: ArcMouseHandler<Datum>;
    onMouseEnter?: ArcMouseHandler<Datum>;
    onMouseMove?: ArcMouseHandler<Datum>;
    onMouseLeave?: ArcMouseHandler<Datum>;
    transitionMode: ArcTransitionMode;
    component?: ArcComponent<Datum>;
}
export declare const ArcsLayer: <Datum extends DatumWithArcAndColor>({ center, data, arcGenerator, borderWidth, borderColor, onClick, onMouseEnter, onMouseMove, onMouseLeave, transitionMode, component, }: ArcsLayerProps<Datum>) => JSX.Element;
export {};
//# sourceMappingURL=ArcsLayer.d.ts.map