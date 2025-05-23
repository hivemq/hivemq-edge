/**
 * Block drag operations outside of `@atlaskit/pragmatic-drag-and-drop`
 */
declare function start(): void;
/**
 * TODO: for next major, we could look at do the following:
 *
 * ```diff
 * - preventUnhandled.start();
 * - preventUnhandled.stop();
 * + const stop = preventUnhandled();
 * ```
 */
declare function stop(): void;
export declare const preventUnhandled: {
    start: typeof start;
    stop: typeof stop;
};
export {};
