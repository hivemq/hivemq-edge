import { Arc, ArcGenerator } from './types';
/**
 * This hook can be used to animate a single arc,
 * if you want to animate a group of arcs,
 * please have a look at the `useArcsTransition` hook.
 */
export declare const useAnimatedArc: (datumWithArc: {
    arc: Arc;
}, arcGenerator: ArcGenerator) => {
    path: import("@react-spring/core").Interpolation<string | null, any>;
    startAngle: import("@react-spring/core").SpringValue<number>;
    endAngle: import("@react-spring/core").SpringValue<number>;
    innerRadius: import("@react-spring/core").SpringValue<number>;
    outerRadius: import("@react-spring/core").SpringValue<number>;
};
//# sourceMappingURL=useAnimatedArc.d.ts.map