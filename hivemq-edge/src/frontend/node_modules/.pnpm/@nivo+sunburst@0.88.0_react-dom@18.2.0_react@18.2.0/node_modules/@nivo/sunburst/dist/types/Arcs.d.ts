/// <reference types="react" />
import { ArcGenerator } from '@nivo/arcs';
import { ComputedDatum, SunburstCommonProps, MouseHandlers } from './types';
interface ArcsProps<RawDatum> {
    center: [number, number];
    data: ComputedDatum<RawDatum>[];
    arcGenerator: ArcGenerator;
    borderWidth: SunburstCommonProps<RawDatum>['borderWidth'];
    borderColor: SunburstCommonProps<RawDatum>['borderColor'];
    isInteractive: SunburstCommonProps<RawDatum>['isInteractive'];
    onClick?: MouseHandlers<RawDatum>['onClick'];
    onMouseEnter?: MouseHandlers<RawDatum>['onMouseEnter'];
    onMouseMove?: MouseHandlers<RawDatum>['onMouseMove'];
    onMouseLeave?: MouseHandlers<RawDatum>['onMouseLeave'];
    tooltip: SunburstCommonProps<RawDatum>['tooltip'];
    transitionMode: SunburstCommonProps<RawDatum>['transitionMode'];
}
export declare const Arcs: <RawDatum>({ center, data, arcGenerator, borderWidth, borderColor, isInteractive, onClick, onMouseEnter, onMouseMove, onMouseLeave, tooltip, transitionMode, }: ArcsProps<RawDatum>) => JSX.Element;
export {};
//# sourceMappingURL=Arcs.d.ts.map