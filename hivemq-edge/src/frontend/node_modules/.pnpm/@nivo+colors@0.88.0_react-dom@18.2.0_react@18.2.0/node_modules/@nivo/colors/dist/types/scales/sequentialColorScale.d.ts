import { ColorInterpolatorId } from '../schemes';
export interface SequentialColorScaleBaseConfig {
    type: 'sequential';
    minValue?: number;
    maxValue?: number;
}
export interface SequentialColorScaleSchemeConfig extends SequentialColorScaleBaseConfig {
    scheme?: ColorInterpolatorId;
}
export interface SequentialColorScaleColorsConfig extends SequentialColorScaleBaseConfig {
    colors: [string, string];
}
export interface SequentialColorScaleInterpolatorConfig extends SequentialColorScaleBaseConfig {
    interpolator: (t: number) => string;
}
export type SequentialColorScaleConfig = SequentialColorScaleSchemeConfig | SequentialColorScaleColorsConfig | SequentialColorScaleInterpolatorConfig;
export interface SequentialColorScaleValues {
    min: number;
    max: number;
}
export declare const sequentialColorScaleDefaults: {
    scheme: ColorInterpolatorId;
};
export declare const getSequentialColorScale: (config: SequentialColorScaleConfig, values: SequentialColorScaleValues) => import("d3-scale").ScaleSequential<string, never>;
export declare const useSequentialColorScale: (config: SequentialColorScaleConfig, values: SequentialColorScaleValues) => import("d3-scale").ScaleSequential<string, never>;
//# sourceMappingURL=sequentialColorScale.d.ts.map