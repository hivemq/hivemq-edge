/* istanbul ignore file -- @preserve */
/**
 * UI Schema for Layout Options
 *
 * Individual UI schemas for each layout algorithm type.
 * Each UI schema is self-contained and specific to its algorithm.
 */

import type { UiSchema } from '@rjsf/utils'

/**
 * Base UI schema with common settings
 */
const baseUISchema: UiSchema = {
  'ui:submitButtonOptions': {
    norender: true,
  },
}

/**
 * UI Schema for Dagre layout (both TB and LR)
 */
export const dagreLayoutUISchema: UiSchema = {
  ...baseUISchema,
  'ui:order': ['ranksep', 'nodesep', 'edgesep', 'ranker', '*', 'animate', 'animationDuration', 'fitView'],
  ranksep: {
    'ui:widget': 'updown',
  },
  nodesep: {
    'ui:widget': 'updown',
  },
  edgesep: {
    'ui:widget': 'updown',
  },
  ranker: {
    'ui:enumNames': ['Network Simplex (default)', 'Tight Tree', 'Longest Path'],
  },
  animate: {
    'ui:widget': 'checkbox',
  },
  animationDuration: {
    'ui:widget': 'updown',
  },
  fitView: {
    'ui:widget': 'checkbox',
  },
}

/**
 * UI Schema for Radial Hub layout
 */
export const radialHubLayoutUISchema: UiSchema = {
  ...baseUISchema,
  'ui:order': ['layerSpacing', 'startAngle', 'centerX', 'centerY', '*', 'animate', 'animationDuration', 'fitView'],
  layerSpacing: {
    'ui:widget': 'updown',
  },
  startAngle: {
    'ui:enumNames': ["Top (12 o'clock)", "Right (3 o'clock)", "Bottom (6 o'clock)", "Left (9 o'clock)"],
  },
  centerX: {
    'ui:widget': 'updown',
  },
  centerY: {
    'ui:widget': 'updown',
  },
  animate: {
    'ui:widget': 'checkbox',
  },
  animationDuration: {
    'ui:widget': 'updown',
  },
  fitView: {
    'ui:widget': 'checkbox',
  },
}

/**
 * UI Schema for WebCola Force-directed layout
 */
export const colaForceLayoutUISchema: UiSchema = {
  ...baseUISchema,
  'ui:order': [
    'linkDistance',
    'maxIterations',
    'convergenceThreshold',
    'avoidOverlaps',
    'handleDisconnected',
    '*',
    'animate',
    'animationDuration',
    'fitView',
  ],
  linkDistance: {
    'ui:widget': 'updown',
  },
  maxIterations: {
    'ui:widget': 'updown',
  },
  convergenceThreshold: {
    'ui:widget': 'updown',
  },
  avoidOverlaps: {
    'ui:widget': 'checkbox',
  },
  handleDisconnected: {
    'ui:widget': 'checkbox',
  },
  animate: {
    'ui:widget': 'checkbox',
  },
  animationDuration: {
    'ui:widget': 'updown',
  },
  fitView: {
    'ui:widget': 'checkbox',
  },
}

/**
 * UI Schema for WebCola Constrained layout
 */
export const colaConstrainedLayoutUISchema: UiSchema = {
  ...baseUISchema,
  'ui:order': ['flowDirection', 'layerGap', 'nodeGap', '*', 'animate', 'animationDuration', 'fitView'],
  flowDirection: {
    'ui:enumNames': ['Vertical (Top to Bottom)', 'Horizontal (Left to Right)'],
  },
  layerGap: {
    'ui:widget': 'updown',
  },
  nodeGap: {
    'ui:widget': 'updown',
  },
  animate: {
    'ui:widget': 'checkbox',
  },
  animationDuration: {
    'ui:widget': 'updown',
  },
  fitView: {
    'ui:widget': 'checkbox',
  },
}

/**
 * UI Schema for Manual layout (no options to display)
 */
export const manualLayoutUISchema: UiSchema = {
  ...baseUISchema,
  'ui:description': 'Manual layout has no configurable options. Nodes remain in their current positions.',
}
