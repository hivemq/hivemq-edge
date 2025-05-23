import { MouseEvent } from 'react';
import { SpringValue, Interpolation } from '@react-spring/web';
import { DatumWithArcAndColor } from './types';
export type ArcMouseHandler<Datum extends DatumWithArcAndColor> = (datum: Datum, event: MouseEvent<SVGPathElement>) => void;
export interface ArcShapeProps<Datum extends DatumWithArcAndColor> {
    datum: Datum;
    style: {
        opacity: SpringValue<number>;
        color: SpringValue<string>;
        borderWidth: number;
        borderColor: SpringValue<string>;
        path: Interpolation<string>;
    };
    onClick?: ArcMouseHandler<Datum>;
    onMouseEnter?: ArcMouseHandler<Datum>;
    onMouseMove?: ArcMouseHandler<Datum>;
    onMouseLeave?: ArcMouseHandler<Datum>;
}
/**
 * A simple arc component to be used typically with an `ArcsLayer`.
 *
 * Please note that the component accepts `SpringValue`s instead of
 * regular values to support animations.
 */
export declare const ArcShape: <Datum extends DatumWithArcAndColor>({ datum, style, onClick, onMouseEnter, onMouseMove, onMouseLeave, }: ArcShapeProps<Datum>) => JSX.Element;
//# sourceMappingURL=ArcShape.d.ts.map