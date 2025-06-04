/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Link } from './Link';

/**
 * List of result items that are returned by this endpoint
 */
export type Notification = {
    /**
     * The notification description
     */
    description?: string | null;
    /**
     * The notification level
     */
    level?: Notification.level;
    link?: Link;
    /**
     * The notification title
     */
    title?: string;
};

export namespace Notification {

    /**
     * The notification level
     */
    export enum level {
        NOTICE = 'NOTICE',
        WARNING = 'WARNING',
        ERROR = 'ERROR',
    }


}

