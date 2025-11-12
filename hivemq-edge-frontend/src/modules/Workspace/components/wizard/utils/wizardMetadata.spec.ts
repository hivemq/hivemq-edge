import { expect } from 'vitest'
import type { Node } from '@xyflow/react'

import {
  getWizardMetadata,
  getWizardStepCount,
  getWizardStep,
  getEntityWizardTypes,
  getIntegrationWizardTypes,
  requiresGhost,
  getWizardIcon,
  getWizardCategory,
  requiresSelection,
  getStepDescriptionKey,
} from './wizardMetadata'
import { EntityType, IntegrationPointType } from '../types'

describe('wizardMetadata', () => {
  describe('getWizardMetadata', () => {
    it('should return metadata for ADAPTER', () => {
      const metadata = getWizardMetadata(EntityType.ADAPTER)

      expect(metadata).toBeDefined()
      expect(metadata.type).toBe(EntityType.ADAPTER)
      expect(metadata.category).toBe('entity')
      expect(metadata.requiresGhost).toBe(true)
    })

    it('should return metadata for BRIDGE', () => {
      const metadata = getWizardMetadata(EntityType.BRIDGE)

      expect(metadata).toBeDefined()
      expect(metadata.type).toBe(EntityType.BRIDGE)
      expect(metadata.requiresGhost).toBe(true)
    })

    it('should return metadata for COMBINER', () => {
      const metadata = getWizardMetadata(EntityType.COMBINER)

      expect(metadata).toBeDefined()
      expect(metadata.type).toBe(EntityType.COMBINER)
      expect(metadata.requiresSelection).toBe(true)
      expect(metadata.requiresGhost).toBe(true)
    })

    it('should return metadata for ASSET_MAPPER', () => {
      const metadata = getWizardMetadata(EntityType.ASSET_MAPPER)

      expect(metadata).toBeDefined()
      expect(metadata.type).toBe(EntityType.ASSET_MAPPER)
      expect(metadata.requiresSelection).toBe(true)
      expect(metadata.requiresGhost).toBe(true)
    })

    it('should return undefined for unknown type', () => {
      const metadata = getWizardMetadata('UNKNOWN' as EntityType)

      expect(metadata).toBeUndefined()
    })
  })

  describe('getWizardStepCount', () => {
    it('should return correct step count for ADAPTER', () => {
      const stepCount = getWizardStepCount(EntityType.ADAPTER)

      expect(stepCount).toBe(3) // Adapter has 3 steps
    })

    it('should return correct step count for BRIDGE', () => {
      const stepCount = getWizardStepCount(EntityType.BRIDGE)

      expect(stepCount).toBe(2) // Review + Configure
    })

    it('should return correct step count for COMBINER', () => {
      const stepCount = getWizardStepCount(EntityType.COMBINER)

      expect(stepCount).toBe(2) // Select + Configure
    })

    it('should return correct step count for ASSET_MAPPER', () => {
      const stepCount = getWizardStepCount(EntityType.ASSET_MAPPER)

      expect(stepCount).toBe(2) // Select + Configure
    })

    it('should handle unknown type', () => {
      // Function will throw error for unknown types since metadata doesn't exist
      expect(() => getWizardStepCount('UNKNOWN' as EntityType)).toThrow()
    })
  })

  describe('getWizardStep', () => {
    it('should return first step for ADAPTER', () => {
      const step = getWizardStep(EntityType.ADAPTER, 0)

      expect(step).toBeDefined()
      expect(step?.index).toBe(0)
      expect(step?.descriptionKey).toBeDefined()
    })

    it('should return step with selection constraints for COMBINER', () => {
      const step = getWizardStep(EntityType.COMBINER, 0)

      expect(step).toBeDefined()
      expect(step?.requiresSelection).toBe(true)
      expect(step?.selectionConstraints).toBeDefined()
      expect(step?.selectionConstraints?.minNodes).toBe(2)
    })

    it('should return step with selection constraints for ASSET_MAPPER', () => {
      const step = getWizardStep(EntityType.ASSET_MAPPER, 0)

      expect(step).toBeDefined()
      expect(step?.requiresSelection).toBe(true)
      expect(step?.selectionConstraints).toBeDefined()
      expect(step?.selectionConstraints?.minNodes).toBe(3) // 1 Pulse + 2 sources
    })

    it('should return configuration step for ADAPTER', () => {
      const step = getWizardStep(EntityType.ADAPTER, 1)

      expect(step).toBeDefined()
      expect(step?.requiresConfiguration).toBe(true)
    })

    it('should return undefined for out of range step', () => {
      const step = getWizardStep(EntityType.ADAPTER, 999)

      expect(step).toBeUndefined()
    })

    it('should handle unknown entity type', () => {
      // Function will throw error for unknown types since metadata doesn't exist
      expect(() => getWizardStep('UNKNOWN' as EntityType, 0)).toThrow()
    })
  })

  describe('getEntityWizardTypes', () => {
    it('should return all entity wizard types', () => {
      const types = getEntityWizardTypes()

      expect(types).toContain(EntityType.ADAPTER)
      expect(types).toContain(EntityType.BRIDGE)
      expect(types).toContain(EntityType.COMBINER)
      expect(types).toContain(EntityType.ASSET_MAPPER)
      expect(types).toContain(EntityType.GROUP)
    })

    it('should not include integration point types', () => {
      const types = getEntityWizardTypes()

      expect(types).not.toContain(IntegrationPointType.TAG)
      expect(types).not.toContain(IntegrationPointType.TOPIC_FILTER)
    })
  })

  describe('getIntegrationWizardTypes', () => {
    it('should return all integration point wizard types', () => {
      const types = getIntegrationWizardTypes()

      expect(types).toContain(IntegrationPointType.TAG)
      expect(types).toContain(IntegrationPointType.TOPIC_FILTER)
      expect(types).toContain(IntegrationPointType.DATA_MAPPING_NORTH)
    })

    it('should not include entity types', () => {
      const types = getIntegrationWizardTypes()

      expect(types).not.toContain(EntityType.ADAPTER)
      expect(types).not.toContain(EntityType.BRIDGE)
    })
  })

  describe('requiresGhost', () => {
    it('should return true for ADAPTER', () => {
      expect(requiresGhost(EntityType.ADAPTER)).toBe(true)
    })

    it('should return true for BRIDGE', () => {
      expect(requiresGhost(EntityType.BRIDGE)).toBe(true)
    })

    it('should return true for COMBINER', () => {
      expect(requiresGhost(EntityType.COMBINER)).toBe(true)
    })

    it('should return true for ASSET_MAPPER', () => {
      expect(requiresGhost(EntityType.ASSET_MAPPER)).toBe(true)
    })

    it('should return true for GROUP', () => {
      expect(requiresGhost(EntityType.GROUP)).toBe(true)
    })

    it('should return false for integration points', () => {
      expect(requiresGhost(IntegrationPointType.TAG)).toBe(false)
      expect(requiresGhost(IntegrationPointType.TOPIC_FILTER)).toBe(false)
    })

    it('should handle unknown type', () => {
      // Function will throw error for unknown types since metadata doesn't exist
      expect(() => requiresGhost('UNKNOWN' as EntityType)).toThrow()
    })
  })

  describe('getWizardIcon', () => {
    it('should return icon component for each entity type', () => {
      const adapterIcon = getWizardIcon(EntityType.ADAPTER)
      const bridgeIcon = getWizardIcon(EntityType.BRIDGE)
      const combinerIcon = getWizardIcon(EntityType.COMBINER)

      expect(adapterIcon).toBeDefined()
      expect(bridgeIcon).toBeDefined()
      expect(combinerIcon).toBeDefined()
    })

    it('should return icon component for integration types', () => {
      const tagIcon = getWizardIcon(IntegrationPointType.TAG)
      const topicFilterIcon = getWizardIcon(IntegrationPointType.TOPIC_FILTER)

      expect(tagIcon).toBeDefined()
      expect(topicFilterIcon).toBeDefined()
    })
  })

  describe('selection constraints', () => {
    it('should have COMBINE capability requirement for COMBINER', () => {
      const step = getWizardStep(EntityType.COMBINER, 0)

      expect(step?.selectionConstraints?.requiresProtocolCapabilities).toContain('COMBINE')
    })

    it('should have COMBINE capability requirement for ASSET_MAPPER', () => {
      const step = getWizardStep(EntityType.ASSET_MAPPER, 0)

      expect(step?.selectionConstraints?.requiresProtocolCapabilities).toContain('COMBINE')
    })

    it('should allow ADAPTER_NODE and BRIDGE_NODE for COMBINER', () => {
      const step = getWizardStep(EntityType.COMBINER, 0)

      expect(step?.selectionConstraints?.allowedNodeTypes).toContain('ADAPTER_NODE')
      expect(step?.selectionConstraints?.allowedNodeTypes).toContain('BRIDGE_NODE')
    })

    it('should allow ADAPTER_NODE and BRIDGE_NODE for ASSET_MAPPER', () => {
      const step = getWizardStep(EntityType.ASSET_MAPPER, 0)

      expect(step?.selectionConstraints?.allowedNodeTypes).toContain('ADAPTER_NODE')
      expect(step?.selectionConstraints?.allowedNodeTypes).toContain('BRIDGE_NODE')
    })
  })

  describe('getWizardCategory', () => {
    it('should return entity category for adapter', () => {
      const category = getWizardCategory(EntityType.ADAPTER)
      expect(category).toBe('entity')
    })

    it('should return integration category for TAG', () => {
      const category = getWizardCategory(IntegrationPointType.TAG)
      expect(category).toBe('integration')
    })
  })

  describe('requiresSelection', () => {
    it('should return true for combiner', () => {
      expect(requiresSelection(EntityType.COMBINER)).toBe(true)
    })

    it('should return false for adapter', () => {
      expect(requiresSelection(EntityType.ADAPTER)).toBe(false)
    })
  })

  describe('getStepDescriptionKey', () => {
    it('should return description key for valid step', () => {
      const key = getStepDescriptionKey(EntityType.ADAPTER, 0)
      expect(key).toBe('step_ADAPTER_0')
    })

    it('should return empty string for invalid step', () => {
      const key = getStepDescriptionKey(EntityType.ADAPTER, 999)
      expect(key).toBe('')
    })
  })

  describe('customFilter in selection constraints', () => {
    it('should allow bridge nodes for COMBINER', () => {
      const metadata = getWizardMetadata(EntityType.COMBINER)
      const step = metadata.steps[0]
      const constraints = step.selectionConstraints

      expect(constraints).toBeDefined()
      expect(constraints?.customFilter).toBeDefined()

      const bridgeNode = { type: 'BRIDGE_NODE', id: 'bridge-1', position: { x: 0, y: 0 }, data: {} } as Node
      const result = constraints?.customFilter?.(bridgeNode)
      expect(result).toBe(true)
    })

    it('should allow adapter nodes for COMBINER', () => {
      const metadata = getWizardMetadata(EntityType.COMBINER)
      const step = metadata.steps[0]
      const constraints = step.selectionConstraints

      const adapterNode = { type: 'ADAPTER_NODE', id: 'adapter-1', position: { x: 0, y: 0 }, data: {} } as Node
      const result = constraints?.customFilter?.(adapterNode)
      expect(result).toBe(true)
    })

    it('should allow bridge nodes for ASSET_MAPPER', () => {
      const metadata = getWizardMetadata(EntityType.ASSET_MAPPER)
      const step = metadata.steps[0]
      const constraints = step.selectionConstraints

      expect(constraints).toBeDefined()
      expect(constraints?.customFilter).toBeDefined()

      const bridgeNode = { type: 'BRIDGE_NODE', id: 'bridge-1', position: { x: 0, y: 0 }, data: {} } as Node
      const result = constraints?.customFilter?.(bridgeNode)
      expect(result).toBe(true)
    })

    it('should allow adapter nodes for ASSET_MAPPER', () => {
      const metadata = getWizardMetadata(EntityType.ASSET_MAPPER)
      const step = metadata.steps[0]
      const constraints = step.selectionConstraints

      const adapterNode = { type: 'ADAPTER_NODE', id: 'adapter-1', position: { x: 0, y: 0 }, data: {} } as Node
      const result = constraints?.customFilter?.(adapterNode)
      expect(result).toBe(true)
    })
  })

  describe('edge cases', () => {
    it('should throw for unknown wizard type in getWizardStep', () => {
      expect(() => getWizardStep('UNKNOWN_TYPE' as EntityType, 0)).toThrow()
    })

    it('should return undefined for out of bounds step index', () => {
      const result = getWizardStep(EntityType.ADAPTER, -1)
      expect(result).toBeUndefined()
    })

    it('should return correct step configuration for all entity types', () => {
      const entityTypes = [
        EntityType.ADAPTER,
        EntityType.BRIDGE,
        EntityType.COMBINER,
        EntityType.ASSET_MAPPER,
        EntityType.GROUP,
      ]

      entityTypes.forEach((type) => {
        const metadata = getWizardMetadata(type)
        expect(metadata).toBeDefined()
        expect(metadata.type).toBe(type)
        expect(metadata.steps.length).toBeGreaterThan(0)

        metadata.steps.forEach((step, index) => {
          expect(step.index).toBe(index)
          expect(step.descriptionKey).toBeTruthy()
        })
      })
    })

    it('should return correct step configuration for all integration types', () => {
      const integrationTypes = [
        IntegrationPointType.TAG,
        IntegrationPointType.TOPIC_FILTER,
        IntegrationPointType.DATA_MAPPING_NORTH,
        IntegrationPointType.DATA_MAPPING_SOUTH,
        IntegrationPointType.DATA_COMBINING,
      ]

      integrationTypes.forEach((type) => {
        const metadata = getWizardMetadata(type)
        expect(metadata).toBeDefined()
        expect(metadata.type).toBe(type)
        expect(metadata.category).toBe('integration')
      })
    })
  })
})
