import { interpolateBrBG, interpolatePRGn, interpolatePiYG, interpolatePuOr, interpolateRdBu, interpolateRdGy, interpolateRdYlBu, interpolateRdYlGn, interpolateSpectral } from 'd3-scale-chromatic';
export declare const divergingColorSchemes: {
    brown_blueGreen: readonly (readonly string[])[];
    purpleRed_green: readonly (readonly string[])[];
    pink_yellowGreen: readonly (readonly string[])[];
    purple_orange: readonly (readonly string[])[];
    red_blue: readonly (readonly string[])[];
    red_grey: readonly (readonly string[])[];
    red_yellow_blue: readonly (readonly string[])[];
    red_yellow_green: readonly (readonly string[])[];
    spectral: readonly (readonly string[])[];
};
export type DivergingColorSchemeId = keyof typeof divergingColorSchemes;
export declare const divergingColorSchemeIds: ("brown_blueGreen" | "purpleRed_green" | "pink_yellowGreen" | "purple_orange" | "red_blue" | "red_grey" | "red_yellow_blue" | "red_yellow_green" | "spectral")[];
export declare const divergingColorInterpolators: {
    brown_blueGreen: typeof interpolateBrBG;
    purpleRed_green: typeof interpolatePRGn;
    pink_yellowGreen: typeof interpolatePiYG;
    purple_orange: typeof interpolatePuOr;
    red_blue: typeof interpolateRdBu;
    red_grey: typeof interpolateRdGy;
    red_yellow_blue: typeof interpolateRdYlBu;
    red_yellow_green: typeof interpolateRdYlGn;
    spectral: typeof interpolateSpectral;
};
export type DivergingColorInterpolatorId = keyof typeof divergingColorInterpolators;
//# sourceMappingURL=diverging.d.ts.map