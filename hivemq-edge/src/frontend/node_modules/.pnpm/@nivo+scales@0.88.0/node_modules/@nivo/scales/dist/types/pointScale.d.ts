import { ScalePoint as D3ScalePoint } from 'd3-scale';
import { ComputedSerieAxis, ScalePoint, ScalePointSpec, StringValue } from './types';
export declare const createPointScale: <Input extends StringValue>(_spec: ScalePointSpec, data: ComputedSerieAxis<Input>, size: number) => ScalePoint<Input>;
export declare const castPointScale: <Input extends StringValue>(scale: D3ScalePoint<Input>) => ScalePoint<Input>;
//# sourceMappingURL=pointScale.d.ts.map