/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * A policy validator which executes the defined validation.
 */
export type DataPolicyValidator = {
    /**
     * The required arguments of the referenced validator type.
     */
    arguments: Record<string, any>;
    /**
     * The type of the validator.
     */
    type: DataPolicyValidator.type;
};

export namespace DataPolicyValidator {

    /**
     * The type of the validator.
     */
    export enum type {
        SCHEMA = 'SCHEMA',
    }


}

