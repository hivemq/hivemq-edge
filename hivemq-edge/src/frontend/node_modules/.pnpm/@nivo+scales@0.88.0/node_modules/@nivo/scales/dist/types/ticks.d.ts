import { ScaleValue, TicksSpec, AnyScale, ScaleWithBandwidth } from './types';
export declare const centerScale: <Value>(scale: ScaleWithBandwidth) => ScaleWithBandwidth | (<T extends Value>(d: T) => number);
export declare const getScaleTicks: <Value extends ScaleValue>(scale: AnyScale, spec?: TicksSpec<Value> | undefined) => any[];
//# sourceMappingURL=ticks.d.ts.map