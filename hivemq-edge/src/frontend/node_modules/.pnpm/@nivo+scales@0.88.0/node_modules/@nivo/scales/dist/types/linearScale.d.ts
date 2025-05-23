import { NumberValue, ScaleLinear as D3ScaleLinear } from 'd3-scale';
import { ScaleLinearSpec, ScaleLinear, ComputedSerieAxis, ScaleAxis } from './types';
export declare const createLinearScale: <Output extends NumberValue>({ min, max, stacked, reverse, clamp, nice, }: ScaleLinearSpec, data: ComputedSerieAxis<Output>, size: number, axis: ScaleAxis) => ScaleLinear<number>;
export declare const castLinearScale: <Range_1, Output>(scale: D3ScaleLinear<Range_1, Output, never>, stacked?: boolean) => ScaleLinear<number>;
//# sourceMappingURL=linearScale.d.ts.map