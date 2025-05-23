import { ColorInterpolatorId } from '../schemes';
export interface QuantizeColorScaleSchemeConfig {
    type: 'quantize';
    domain?: [number, number];
    scheme?: ColorInterpolatorId;
    steps?: number;
}
export interface QuantizeColorScaleColorsConfig {
    type: 'quantize';
    domain?: [number, number];
    colors: string[];
}
export type QuantizeColorScaleConfig = QuantizeColorScaleSchemeConfig | QuantizeColorScaleColorsConfig;
export interface QuantizeColorScaleValues {
    min: number;
    max: number;
}
export declare const quantizeColorScaleDefaults: {
    scheme: ColorInterpolatorId;
    steps: NonNullable<QuantizeColorScaleSchemeConfig['steps']>;
};
export declare const getQuantizeColorScale: (config: QuantizeColorScaleConfig, values: QuantizeColorScaleValues) => import("d3-scale").ScaleQuantize<string, never>;
export declare const useQuantizeColorScale: (config: QuantizeColorScaleConfig, values: QuantizeColorScaleValues) => import("d3-scale").ScaleQuantize<string, never>;
//# sourceMappingURL=quantizeColorScale.d.ts.map