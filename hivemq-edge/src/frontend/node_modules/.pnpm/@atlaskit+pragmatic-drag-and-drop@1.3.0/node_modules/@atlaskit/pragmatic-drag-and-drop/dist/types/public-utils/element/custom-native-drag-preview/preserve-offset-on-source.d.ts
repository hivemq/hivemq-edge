import { type Input } from '../../../entry-point/types';
import type { GetOffsetFn } from './types';
export declare function preserveOffsetOnSource({ element, input, }: {
    element: HTMLElement;
    input: Input;
}): GetOffsetFn;
