/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ApiErrorMessage = {
    properties: {
        detail: {
            type: 'string',
            description: `Detailed contextual description of this error`,
        },
        fieldName: {
            type: 'string',
            description: `Application Error Code associate with this field`,
        },
        title: {
            type: 'string',
            description: `The title of this error`,
        },
    },
} as const;
