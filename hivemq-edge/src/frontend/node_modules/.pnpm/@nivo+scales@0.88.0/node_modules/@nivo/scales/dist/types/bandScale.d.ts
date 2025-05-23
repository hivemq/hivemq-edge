import { ScaleBand as D3ScaleBand } from 'd3-scale';
import { ComputedSerieAxis, ScaleBand, ScaleBandSpec, StringValue, ScaleAxis } from './types';
export declare const createBandScale: <Input extends StringValue>({ round }: ScaleBandSpec, data: ComputedSerieAxis<Input>, size: number, axis: ScaleAxis) => ScaleBand<Input>;
export declare const castBandScale: <Input extends StringValue>(scale: D3ScaleBand<Input>) => ScaleBand<Input>;
//# sourceMappingURL=bandScale.d.ts.map