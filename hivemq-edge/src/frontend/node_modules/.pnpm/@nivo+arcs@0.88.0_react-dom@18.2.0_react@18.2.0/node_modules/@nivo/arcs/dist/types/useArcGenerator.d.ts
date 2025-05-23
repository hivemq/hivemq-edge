import { ArcGenerator } from './types';
/**
 * Memoize a d3 arc generator.
 *
 * Please note that both inner/outer radius aren't static
 * and should come from the arc itself, while it requires
 * more props on the arcs, it provides more flexibility
 * because it's not limited to pie then but can also work
 * with charts such as sunbursts.
 */
export declare const useArcGenerator: ({ cornerRadius, padAngle, }?: {
    cornerRadius?: number | undefined;
    padAngle?: number | undefined;
}) => ArcGenerator;
//# sourceMappingURL=useArcGenerator.d.ts.map