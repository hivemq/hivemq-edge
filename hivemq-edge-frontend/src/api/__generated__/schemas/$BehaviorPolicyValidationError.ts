/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $BehaviorPolicyValidationError = {
    type: 'all-of',
    contains: [{
        type: 'ValidationError',
    }, {
        type: 'one-of',
        contains: [{
            type: 'IllegalEventTransitionValidationError',
        }, {
            type: 'IllegalFunctionValidationError',
        }, {
            type: 'InvalidSchemaVersionValidationError',
        }, {
            type: 'AtLeastOneFieldMissingValidationError',
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
