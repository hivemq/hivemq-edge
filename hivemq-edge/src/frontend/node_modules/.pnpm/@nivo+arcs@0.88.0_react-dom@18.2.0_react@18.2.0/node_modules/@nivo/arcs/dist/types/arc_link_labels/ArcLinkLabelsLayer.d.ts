/// <reference types="react" />
import { DatumWithArcAndColor } from '../types';
import { ArcLinkLabelsProps } from './props';
import { ArcLinkLabelProps } from './ArcLinkLabel';
export type ArcLinkLabelComponent<Datum extends DatumWithArcAndColor> = (props: ArcLinkLabelProps<Datum>) => JSX.Element;
interface ArcLinkLabelsLayerProps<Datum extends DatumWithArcAndColor> {
    center: [number, number];
    data: Datum[];
    label: ArcLinkLabelsProps<Datum>['arcLinkLabel'];
    skipAngle: ArcLinkLabelsProps<Datum>['arcLinkLabelsSkipAngle'];
    offset: ArcLinkLabelsProps<Datum>['arcLinkLabelsOffset'];
    diagonalLength: ArcLinkLabelsProps<Datum>['arcLinkLabelsDiagonalLength'];
    straightLength: ArcLinkLabelsProps<Datum>['arcLinkLabelsStraightLength'];
    strokeWidth: ArcLinkLabelsProps<Datum>['arcLinkLabelsThickness'];
    textOffset: ArcLinkLabelsProps<Datum>['arcLinkLabelsTextOffset'];
    textColor: ArcLinkLabelsProps<Datum>['arcLinkLabelsTextColor'];
    linkColor: ArcLinkLabelsProps<Datum>['arcLinkLabelsColor'];
    component?: ArcLinkLabelComponent<Datum>;
}
export declare const ArcLinkLabelsLayer: <Datum extends DatumWithArcAndColor>({ center, data, label: labelAccessor, skipAngle, offset, diagonalLength, straightLength, strokeWidth, textOffset, textColor, linkColor, component, }: ArcLinkLabelsLayerProps<Datum>) => JSX.Element;
export {};
//# sourceMappingURL=ArcLinkLabelsLayer.d.ts.map