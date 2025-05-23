import { Arc } from './types';
/**
 * Check if cursor is in given ring.
 */
export declare const isCursorInRing: (centerX: number, centerY: number, radius: number, innerRadius: number, cursorX: number, cursorY: number) => boolean;
/**
 * Search for an arc being under cursor.
 */
export declare const findArcUnderCursor: <A extends Arc = Arc>(centerX: number, centerY: number, radius: number, innerRadius: number, arcs: A[], cursorX: number, cursorY: number) => A | undefined;
//# sourceMappingURL=interactivity.d.ts.map