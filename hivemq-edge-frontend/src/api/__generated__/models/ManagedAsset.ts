/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Asset } from './Asset';
import type { AssetMapping } from './AssetMapping';

/**
 * The definition of an extended asset, as managed in Edge for the Pulse Client
 */
export type ManagedAsset = (Asset & {
    mapping?: AssetMapping;
});

