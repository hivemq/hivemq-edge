/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Instruction } from './Instruction';
import type { Metadata } from './Metadata';

/**
 * Defines how incoming data should be transformed before being sent out.
 */
export type FieldMapping = {
    /**
     * List of instructions to be applied to incoming data
     */
    instructions: Array<Instruction>;
    metadata: Metadata;
};

