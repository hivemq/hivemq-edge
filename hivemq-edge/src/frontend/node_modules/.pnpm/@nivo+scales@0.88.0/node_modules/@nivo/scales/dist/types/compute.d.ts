import { ScaleAxis, ScaleSpec, ScaleValue, SerieAxis, ComputedSerieAxis } from './types';
type XY = ReturnType<typeof generateSeriesXY>;
type StackedXY = {
    [K in keyof XY]: XY[K] & {
        maxStacked: number;
        minStacked: number;
    };
};
interface SerieDatum {
    x: number | string | Date;
    xStacked?: number | null;
    y: number | string | Date;
    yStacked?: number | null;
}
type Serie<S = never, D extends SerieDatum = SerieDatum> = S & {
    data: D[];
};
type NestedSerie<S = never, D extends SerieDatum = SerieDatum> = S & {
    data: {
        data: D;
    }[];
};
export type ComputedSerie<S = never, D extends SerieDatum = SerieDatum> = S & {
    data: {
        data: D;
        position: {
            x: number | null;
            y: number | null;
        };
    }[];
};
export declare const getOtherAxis: (axis: ScaleAxis) => ScaleAxis;
export declare const compareValues: (a: string | number, b: string | number) => boolean;
export declare const compareDateValues: (a: Date, b: Date) => boolean;
export declare function computeScale<Input extends ScaleValue>(spec: ScaleSpec, data: ComputedSerieAxis<any>, size: number, axis: ScaleAxis): import("./types").ScaleLog | import("./types").ScaleSymlog | import("./types").ScaleLinear<number> | import("./types").ScalePoint<Input> | import("./types").ScaleBand<Input> | import("./types").ScaleTime<Date | import("d3-scale").NumberValue>;
/**
 * Compute x/y d3 scales from an array of data series, and scale specifications.
 *
 * We use generics as it's not uncommon to have extra properties such as an id
 * added to the series, or extra props on data, in such case, you should override
 * the default types.
 */
export declare const computeXYScalesForSeries: <S = never, D extends SerieDatum = SerieDatum>(series: Serie<S, D>[], xScaleSpec: ScaleSpec, yScaleSpec: ScaleSpec, width: number, height: number) => {
    series: ComputedSerie<S, D>[];
    xScale: import("./types").ScaleLog | import("./types").ScaleSymlog | import("./types").ScaleLinear<number> | import("./types").ScaleTime<Date | import("d3-scale").NumberValue> | import("./types").ScalePoint<D["x"]> | import("./types").ScaleBand<D["x"]>;
    yScale: import("./types").ScaleLog | import("./types").ScaleSymlog | import("./types").ScaleLinear<number> | import("./types").ScaleTime<Date | import("d3-scale").NumberValue> | import("./types").ScalePoint<D["y"]> | import("./types").ScaleBand<D["y"]>;
    x: {
        all: unknown[];
        min: unknown;
        max: unknown;
    };
    y: {
        all: unknown[];
        min: unknown;
        max: unknown;
    };
};
export declare const generateSeriesXY: <S = never, D extends SerieDatum = SerieDatum>(series: NestedSerie<S, D>[], xScaleSpec: ScaleSpec, yScaleSpec: ScaleSpec) => {
    x: {
        all: unknown[];
        min: unknown;
        max: unknown;
    };
    y: {
        all: unknown[];
        min: unknown;
        max: unknown;
    };
};
/**
 * Normalize data according to scale type, (time => Date, linear => Number)
 * compute sorted unique values and min/max.
 */
export declare const generateSeriesAxis: <Axis extends ScaleAxis, Value extends ScaleValue>(series: SerieAxis<Axis, Value>, axis: Axis, scaleSpec: ScaleSpec, { getValue, setValue, }?: {
    getValue?: ((d: {
        data: Record<Axis, Value | null>;
    }) => Value | null) | undefined;
    setValue?: ((d: {
        data: Record<Axis, Value | null>;
    }, v: Value) => void) | undefined;
}) => {
    all: unknown[];
    min: unknown;
    max: unknown;
};
export declare const stackAxis: <S = never, D extends SerieDatum = SerieDatum>(axis: ScaleAxis, xy: StackedXY, series: NestedSerie<S, D>[]) => void;
export {};
//# sourceMappingURL=compute.d.ts.map