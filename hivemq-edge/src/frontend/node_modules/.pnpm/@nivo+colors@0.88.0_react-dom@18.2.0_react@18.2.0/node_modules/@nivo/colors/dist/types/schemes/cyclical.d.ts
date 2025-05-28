import { interpolateRainbow, interpolateSinebow } from 'd3-scale-chromatic';
export declare const cyclicalColorInterpolators: {
    rainbow: typeof interpolateRainbow;
    sinebow: typeof interpolateSinebow;
};
export type CyclicalColorInterpolatorId = keyof typeof cyclicalColorInterpolators;
//# sourceMappingURL=cyclical.d.ts.map