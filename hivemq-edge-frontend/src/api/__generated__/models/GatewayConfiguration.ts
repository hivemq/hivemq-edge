/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { EnvironmentProperties } from './EnvironmentProperties';
import type { ExtensionList } from './ExtensionList';
import type { FirstUseInformation } from './FirstUseInformation';
import type { Link } from './Link';
import type { LinkList } from './LinkList';
import type { ModuleList } from './ModuleList';

export type GatewayConfiguration = {
    cloudLink?: Link;
    ctas?: LinkList;
    documentationLink?: Link;
    environment?: EnvironmentProperties;
    extensions?: ExtensionList;
    firstUseInformation?: FirstUseInformation;
    gitHubLink?: Link;
    /**
     * The current id of hivemq edge. Changes at restart.
     */
    hivemqId?: string;
    modules?: ModuleList;
    resources?: LinkList;
    /**
     * Is the tracking of user actions allowed.
     */
    trackingAllowed?: boolean;
};

