import { NumberValue } from 'd3-scale';
import { ComputedSerieAxis, ScaleTime, ScaleTimeSpec } from './types';
export declare const createTimeScale: <Input extends Date | NumberValue>({ format, precision, min, max, useUTC, nice, }: ScaleTimeSpec, data: ComputedSerieAxis<string | Date>, size: number) => ScaleTime<Input>;
//# sourceMappingURL=timeScale.d.ts.map