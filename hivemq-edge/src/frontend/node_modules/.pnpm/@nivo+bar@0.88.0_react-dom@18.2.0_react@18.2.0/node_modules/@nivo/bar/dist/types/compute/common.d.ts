import { ScaleBandSpec, ScaleBand } from '@nivo/scales';
import { BarCommonProps, BarDatum } from '../types';
/**
 * Generates indexed scale.
 */
export declare const getIndexScale: <RawDatum>(data: readonly RawDatum[], getIndex: (datum: RawDatum) => string, padding: number, indexScale: ScaleBandSpec, size: number, axis: 'x' | 'y') => ScaleBand<string>;
/**
 * This method ensures all the provided keys exist in the entire series.
 */
export declare const normalizeData: <RawDatum>(data: readonly RawDatum[], keys: readonly string[]) => RawDatum[];
export declare const filterNullValues: <RawDatum extends Record<string, unknown>>(data: RawDatum) => Exclude<RawDatum, false | "" | 0 | null | undefined>;
export declare const coerceValue: <T>(value: T) => readonly [T, number];
export type BarLabelLayout = {
    labelX: number;
    labelY: number;
    textAnchor: 'start' | 'middle' | 'end';
};
/**
 * Compute the label position and alignment based on a given position and offset.
 */
export declare function useComputeLabelLayout<RawDatum extends BarDatum>(layout?: BarCommonProps<RawDatum>['layout'], reverse?: BarCommonProps<RawDatum>['reverse'], labelPosition?: BarCommonProps<RawDatum>['labelPosition'], labelOffset?: BarCommonProps<RawDatum>['labelOffset']): (width: number, height: number) => BarLabelLayout;
//# sourceMappingURL=common.d.ts.map