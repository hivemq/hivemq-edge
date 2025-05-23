import { ScaleDiverging, ScaleQuantize, ScaleSequential } from 'd3-scale';
import { SequentialColorScaleConfig, SequentialColorScaleValues } from './sequentialColorScale';
import { DivergingColorScaleConfig, DivergingColorScaleValues } from './divergingColorScale';
import { QuantizeColorScaleConfig, QuantizeColorScaleValues } from './quantizeColorScale';
export type ContinuousColorScaleConfig = SequentialColorScaleConfig | DivergingColorScaleConfig | QuantizeColorScaleConfig;
export type ContinuousColorScaleValues = SequentialColorScaleValues | DivergingColorScaleValues | QuantizeColorScaleValues;
export declare const getContinuousColorScale: <Config extends ContinuousColorScaleConfig>(config: Config, values: ContinuousColorScaleValues) => ScaleSequential<string, never> | ScaleDiverging<string, never> | ScaleQuantize<string, never>;
export declare const useContinuousColorScale: (config: ContinuousColorScaleConfig, values: ContinuousColorScaleValues) => ScaleSequential<string, never> | ScaleDiverging<string, never> | ScaleQuantize<string, never>;
export declare const computeContinuousColorScaleColorStops: (scale: ScaleSequential<string> | ScaleDiverging<string> | ScaleQuantize<string>, steps?: number) => {
    key: string;
    offset: number;
    stopColor: string;
}[];
//# sourceMappingURL=continuousColorScale.d.ts.map