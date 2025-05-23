import { SpringValue } from '@react-spring/web';
import { ArcGenerator } from './types';
/**
 * Directly animating paths for arcs leads to sub-optimal results
 * as the interpolation is going to be linear while we deal with polar coordinates,
 * this interpolator is going to generate proper arc transitions.
 * It should be used with the `useAnimatedArc` or `useArcsTransition` hooks.
 */
export declare const interpolateArc: (startAngleValue: SpringValue<number>, endAngleValue: SpringValue<number>, innerRadiusValue: SpringValue<number>, outerRadiusValue: SpringValue<number>, arcGenerator: ArcGenerator) => import("@react-spring/core").Interpolation<string | null, any>;
//# sourceMappingURL=interpolateArc.d.ts.map