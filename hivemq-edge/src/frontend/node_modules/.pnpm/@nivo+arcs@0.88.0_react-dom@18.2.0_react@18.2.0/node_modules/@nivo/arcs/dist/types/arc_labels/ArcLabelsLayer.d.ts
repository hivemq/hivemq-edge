/// <reference types="react" />
import { PropertyAccessor } from '@nivo/core';
import { ArcTransitionMode } from '../arcTransitionMode';
import { DatumWithArcAndColor } from '../types';
import { ArcLabelsProps } from './props';
import { ArcLabelProps } from './ArcLabel';
export type ArcLabelComponent<Datum extends DatumWithArcAndColor> = (props: ArcLabelProps<Datum>) => JSX.Element;
interface ArcLabelsLayerProps<Datum extends DatumWithArcAndColor> {
    center: [number, number];
    data: Datum[];
    label: PropertyAccessor<Datum, string>;
    radiusOffset: ArcLabelsProps<Datum>['arcLabelsRadiusOffset'];
    skipAngle: ArcLabelsProps<Datum>['arcLabelsSkipAngle'];
    textColor: ArcLabelsProps<Datum>['arcLabelsTextColor'];
    transitionMode: ArcTransitionMode;
    component?: ArcLabelsProps<Datum>['arcLabelsComponent'];
}
export declare const ArcLabelsLayer: <Datum extends DatumWithArcAndColor>({ center, data, transitionMode, label: labelAccessor, radiusOffset, skipAngle, textColor, component, }: ArcLabelsLayerProps<Datum>) => JSX.Element;
export {};
//# sourceMappingURL=ArcLabelsLayer.d.ts.map