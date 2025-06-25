/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { PolicyType } from './PolicyType';

export type InterpolationVariable = {
    /**
     * The unique variable name
     */
    variable: string;
    type: InterpolationVariable.type;
    /**
     * The description of the variable name
     */
    description: string;
    /**
     * The list of policy types this variable can be used with
     */
    policyType: Array<PolicyType>;
};

export namespace InterpolationVariable {

    export enum type {
        STRING = 'string',
        LONG = 'long',
    }


}

