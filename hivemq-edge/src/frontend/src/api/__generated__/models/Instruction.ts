/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * List of instructions to be applied to incoming data
 */
export type Instruction = {
    /**
     * The field in the output object where the data will be written to
     */
    destination: string;
    /**
     * The field in the input object where the data will be read from
     */
    source: string;
};

