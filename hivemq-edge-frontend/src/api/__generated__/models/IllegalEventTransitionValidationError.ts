/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ValidationError } from './ValidationError';

export type IllegalEventTransitionValidationError = (ValidationError & {
    /**
     * The event name.
     */
    event: string;
    /**
     * The event from state.
     */
    fromState: string;
    /**
     * The event id.
     */
    id: string;
    /**
     * The path.
     */
    path: string;
    /**
     * The event to state.
     */
    toState: string;
});

