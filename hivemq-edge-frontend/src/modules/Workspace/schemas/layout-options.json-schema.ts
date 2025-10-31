/* istanbul ignore file -- @preserve */
/**
 * JSON Schema for Layout Options
 *
 * Individual schemas for each layout algorithm type.
 * Each schema is self-contained and specific to its algorithm.
 */

import type { JSONSchema7 } from 'json-schema'

/**
 * Common options shared across all layouts (except Manual)
 */
const commonLayoutOptions: Record<string, JSONSchema7> = {
  animate: {
    type: 'boolean',
    title: 'Enable Animation',
    description: 'Animate node transitions when layout is applied',
    default: true,
  },
  animationDuration: {
    type: 'number',
    title: 'Animation Duration (ms)',
    description: 'Duration of the animation in milliseconds',
    minimum: 0,
    maximum: 1000,
    default: 300,
  },
  fitView: {
    type: 'boolean',
    title: 'Fit View',
    description: 'Automatically fit the view to show all nodes after layout',
    default: true,
  },
}

/**
 * Schema for Dagre layout options (both TB and LR)
 */
export const dagreLayoutSchema: JSONSchema7 = {
  type: 'object',
  required: [],
  properties: {
    ranksep: {
      type: 'number',
      title: 'Rank Separation (px)',
      description: 'Vertical space between ranks/layers',
      minimum: 50,
      maximum: 500,
      default: 150,
    },
    nodesep: {
      type: 'number',
      title: 'Node Separation (px)',
      description: 'Horizontal space between nodes in same rank',
      minimum: 20,
      maximum: 200,
      default: 80,
    },
    edgesep: {
      type: 'number',
      title: 'Edge Separation (px)',
      description: 'Space between edges',
      minimum: 10,
      maximum: 100,
      default: 20,
    },
    ranker: {
      type: 'string',
      title: 'Ranker Algorithm',
      description: 'Algorithm for rank assignment',
      enum: ['network-simplex', 'tight-tree', 'longest-path'],
      default: 'network-simplex',
    },
    ...commonLayoutOptions,
  },
}

/**
 * Schema for Radial Hub layout options
 */
export const radialHubLayoutSchema: JSONSchema7 = {
  type: 'object',
  required: [],
  properties: {
    layerSpacing: {
      type: 'number',
      title: 'Layer Spacing (px)',
      description: 'Distance between concentric layers (accounts for node width)',
      minimum: 200,
      maximum: 800,
      default: 500,
    },
    startAngle: {
      type: 'number',
      title: 'Start Angle',
      description: 'Starting position for first node (in radians)',
      enum: [-Math.PI / 2, 0, Math.PI / 2, Math.PI],
      default: -Math.PI / 2,
    },
    centerX: {
      type: 'number',
      title: 'Center X (px)',
      description: 'X coordinate of the center point',
      minimum: 0,
      maximum: 2000,
      default: 400,
    },
    centerY: {
      type: 'number',
      title: 'Center Y (px)',
      description: 'Y coordinate of the center point',
      minimum: 0,
      maximum: 2000,
      default: 300,
    },
    ...commonLayoutOptions,
  },
}

/**
 * Schema for WebCola Force-directed layout options
 */
export const colaForceLayoutSchema: JSONSchema7 = {
  type: 'object',
  required: [],
  properties: {
    linkDistance: {
      type: 'number',
      title: 'Link Distance (px)',
      description: 'Target distance between connected nodes',
      minimum: 200,
      maximum: 800,
      default: 350,
    },
    maxIterations: {
      type: 'number',
      title: 'Max Iterations',
      description: 'Maximum simulation steps (higher = slower)',
      minimum: 100,
      maximum: 5000,
      default: 1000,
    },
    convergenceThreshold: {
      type: 'number',
      title: 'Convergence Threshold',
      description: 'Stop when changes are smaller than this',
      minimum: 0.001,
      maximum: 0.1,
      default: 0.01,
    },
    avoidOverlaps: {
      type: 'boolean',
      title: 'Avoid Overlaps',
      description: 'Prevent nodes from overlapping',
      default: true,
    },
    handleDisconnected: {
      type: 'boolean',
      title: 'Handle Disconnected',
      description: 'Handle disconnected subgraphs',
      default: true,
    },
    ...commonLayoutOptions,
  },
}

/**
 * Schema for WebCola Constrained layout options
 */
export const colaConstrainedLayoutSchema: JSONSchema7 = {
  type: 'object',
  required: [],
  properties: {
    flowDirection: {
      type: 'string',
      title: 'Flow Direction',
      description: 'Direction of hierarchy flow',
      enum: ['y', 'x'],
      default: 'y',
    },
    layerGap: {
      type: 'number',
      title: 'Layer Gap (px)',
      description: 'Space between layers',
      minimum: 200,
      maximum: 800,
      default: 350,
    },
    nodeGap: {
      type: 'number',
      title: 'Node Gap (px)',
      description: 'Space between nodes in same layer',
      minimum: 200,
      maximum: 600,
      default: 300,
    },
    ...commonLayoutOptions,
  },
}

/**
 * Schema for Manual layout (no options)
 */
export const manualLayoutSchema: JSONSchema7 = {
  type: 'object',
  required: [],
  properties: {},
}
