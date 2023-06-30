/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Module } from './Module';

/**
 * The modules available for installation
 */
export type ModuleList = {
    /**
     * List of result items that are returned by this endpoint
     */
    items?: Array<Module>;
};

