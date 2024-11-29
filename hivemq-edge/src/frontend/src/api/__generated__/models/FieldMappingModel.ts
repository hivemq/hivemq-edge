/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { TransformationModel } from './TransformationModel';

export type FieldMappingModel = {
    /**
     * The field name in the outgoing data
     */
    destination?: string;
    /**
     * The field name in the incoming data.
     */
    source?: string;
    transformation?: TransformationModel;
};

