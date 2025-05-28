import { Arc } from '../types';
import { ArcLink } from './types';
/**
 * Compute text anchor for a given arc.
 *
 * `computeArcLink` already computes a `side`, but when using
 * `react-spring`, you cannot have a single interpolation
 * returning several output values, so we need to compute
 * them in separate interpolations.
 */
export declare const computeArcLinkTextAnchor: (arc: Arc) => 'start' | 'end';
/**
 * Compute the link of a single arc, returning its points,
 * please note that points coordinates are relative to
 * the center of the arc.
 */
export declare const computeArcLink: (arc: Arc, offset: number, diagonalLength: number, straightLength: number) => ArcLink;
//# sourceMappingURL=compute.d.ts.map