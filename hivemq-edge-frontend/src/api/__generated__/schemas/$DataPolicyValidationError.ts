/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $DataPolicyValidationError = {
    type: 'all-of',
    contains: [{
        type: 'ValidationError',
    }, {
        type: 'one-of',
        contains: [{
            type: 'AtMostOneFunctionValidationError',
        }, {
            type: 'FunctionMustBePairedValidationError',
        }, {
            type: 'InvalidFunctionOrderValidationError',
        }, {
            type: 'InvalidSchemaVersionValidationError',
        }, {
            type: 'UnknownVariableValidationError',
        }, {
            type: 'EmptyFieldValidationError',
        }, {
            type: 'InvalidFieldLengthValidationError',
        }, {
            type: 'InvalidFieldValueValidationError',
        }, {
            type: 'InvalidIdentifierValidationError',
        }, {
            type: 'MissingFieldValidationError',
        }, {
            type: 'UnsupportedFieldValidationError',
        }],
    }],
} as const;
