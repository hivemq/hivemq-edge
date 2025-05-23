import { type CleanupFn } from '../internal-types';
/** Create a new combined function that will call all the provided functions */
export declare function combine(...fns: CleanupFn[]): CleanupFn;
