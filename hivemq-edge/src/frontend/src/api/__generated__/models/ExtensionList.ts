/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Extension } from './Extension';

/**
 * The extensions available for installation
 */
export type ExtensionList = {
    /**
     * List of result items that are returned by this endpoint
     */
    items?: Array<Extension>;
};

