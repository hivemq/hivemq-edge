import { ColorInterpolatorId } from '../schemes';
interface DivergingColorScaleBaseConfig {
    type: 'diverging';
    minValue?: number;
    maxValue?: number;
    divergeAt?: number;
}
export interface DivergingColorScaleSchemeConfig extends DivergingColorScaleBaseConfig {
    scheme?: ColorInterpolatorId;
}
export interface DivergingColorScaleColorsConfig extends DivergingColorScaleBaseConfig {
    colors: [string, string, string];
}
export interface DivergingColorScaleInterpolatorConfig extends DivergingColorScaleBaseConfig {
    interpolator: (t: number) => string;
}
export type DivergingColorScaleConfig = DivergingColorScaleSchemeConfig | DivergingColorScaleColorsConfig | DivergingColorScaleInterpolatorConfig;
export interface DivergingColorScaleValues {
    min: number;
    max: number;
}
export declare const divergingColorScaleDefaults: {
    scheme: ColorInterpolatorId;
    divergeAt: number;
};
export declare const getDivergingColorScale: (config: DivergingColorScaleConfig, values: DivergingColorScaleValues) => import("d3-scale").ScaleDiverging<string, never>;
export declare const useDivergingColorScale: (config: DivergingColorScaleConfig, values: DivergingColorScaleValues) => import("d3-scale").ScaleDiverging<string, never>;
export {};
//# sourceMappingURL=divergingColorScale.d.ts.map