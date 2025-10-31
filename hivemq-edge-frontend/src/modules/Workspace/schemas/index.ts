/**
 * Workspace Layout Schemas
 *
 * JSON Schema and UI Schema exports for layout options.
 * Each layout algorithm has its own dedicated schema and UI schema.
 */

// JSON Schemas
export {
  dagreLayoutSchema,
  radialHubLayoutSchema,
  colaForceLayoutSchema,
  colaConstrainedLayoutSchema,
  manualLayoutSchema,
} from './layout-options.json-schema'

// UI Schemas
export {
  dagreLayoutUISchema,
  radialHubLayoutUISchema,
  colaForceLayoutUISchema,
  colaConstrainedLayoutUISchema,
  manualLayoutUISchema,
} from './layout-options.ui-schema'
