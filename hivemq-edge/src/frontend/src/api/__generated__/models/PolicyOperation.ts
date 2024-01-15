/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * The pipeline to execute when this action is triggered. The operations in the pipeline are executed in order.
 */
export type PolicyOperation = {
    /**
     * The required arguments of the referenced function.
     */
    arguments: Record<string, any>;
    /**
     * The unique id of the referenced function to execute in this operation.
     */
    functionId: string;
    /**
     * The unique id of the operation in the pipeline.
     */
    id: string;
};

