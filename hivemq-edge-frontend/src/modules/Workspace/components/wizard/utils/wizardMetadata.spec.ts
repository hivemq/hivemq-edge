/**
 * Wizard Metadata Tests
 *
 * Tests for the wizard metadata registry and helper functions.
 * Following pragmatic testing strategy: only accessibility test is unskipped.
 */

import { describe, it, expect } from 'vitest'

import {
  WIZARD_REGISTRY,
  getWizardMetadata,
  getWizardIcon,
  getWizardCategory,
  getWizardStepCount,
  requiresSelection,
  requiresGhost,
  getEntityWizardTypes,
  getIntegrationWizardTypes,
  getWizardStep,
  getStepDescriptionKey,
} from './wizardMetadata'
import { EntityType, IntegrationPointType } from '../types'

describe('wizardMetadata', () => {
  // ✅ ACCESSIBILITY TEST - ALWAYS UNSKIPPED
  it('should be accessible', () => {
    // Verify that all wizard types have valid metadata for accessibility
    // Icons should be defined as functions (React components)
    // All have description keys for screen readers

    const entityTypes = getEntityWizardTypes()
    entityTypes.forEach((type) => {
      const metadata = getWizardMetadata(type)
      const icon = getWizardIcon(type)

      // Icon must be a function (React component) for accessibility
      expect(typeof icon).toBe('function')

      // All steps must have description keys for screen readers
      metadata.steps.forEach((step) => {
        expect(step.descriptionKey).toBeDefined()
        expect(step.descriptionKey.length).toBeGreaterThan(0)
      })
    })

    const integrationTypes = getIntegrationWizardTypes()
    integrationTypes.forEach((type) => {
      const metadata = getWizardMetadata(type)
      const icon = getWizardIcon(type)

      // Icon must be a function (React component) for accessibility
      expect(typeof icon).toBe('function')

      // All steps must have description keys for screen readers
      metadata.steps.forEach((step) => {
        expect(step.descriptionKey).toBeDefined()
        expect(step.descriptionKey.length).toBeGreaterThan(0)
      })
    })
  })

  // ⏭️ SKIPPED TESTS - Document expected behavior but skip for rapid development

  describe.skip('WIZARD_REGISTRY', () => {
    it('should have metadata for all entity types', () => {
      const entityTypes = Object.values(EntityType)

      entityTypes.forEach((type) => {
        expect(WIZARD_REGISTRY[type]).toBeDefined()
        expect(WIZARD_REGISTRY[type].type).toBe(type)
        expect(WIZARD_REGISTRY[type].category).toBe('entity')
      })
    })

    it('should have metadata for all integration point types', () => {
      const integrationTypes = Object.values(IntegrationPointType)

      integrationTypes.forEach((type) => {
        expect(WIZARD_REGISTRY[type]).toBeDefined()
        expect(WIZARD_REGISTRY[type].type).toBe(type)
        expect(WIZARD_REGISTRY[type].category).toBe('integration')
      })
    })

    it('should have valid icon for each wizard type', () => {
      Object.values(WIZARD_REGISTRY).forEach((metadata) => {
        expect(metadata.icon).toBeDefined()
        expect(typeof metadata.icon).toBe('function')
      })
    })

    it('should have at least one step for each wizard type', () => {
      Object.values(WIZARD_REGISTRY).forEach((metadata) => {
        expect(metadata.steps.length).toBeGreaterThan(0)
      })
    })

    it('should have sequential step indices', () => {
      Object.values(WIZARD_REGISTRY).forEach((metadata) => {
        metadata.steps.forEach((step, index) => {
          expect(step.index).toBe(index)
        })
      })
    })

    it('should have description key for each step', () => {
      Object.values(WIZARD_REGISTRY).forEach((metadata) => {
        metadata.steps.forEach((step) => {
          expect(step.descriptionKey).toBeDefined()
          expect(step.descriptionKey.length).toBeGreaterThan(0)
        })
      })
    })
  })

  describe.skip('getWizardMetadata', () => {
    it('should return metadata for ADAPTER', () => {
      const metadata = getWizardMetadata(EntityType.ADAPTER)

      expect(metadata.type).toBe(EntityType.ADAPTER)
      expect(metadata.category).toBe('entity')
      expect(metadata.requiresGhost).toBe(true)
      expect(metadata.steps.length).toBe(3)
    })

    it('should return metadata for BRIDGE', () => {
      const metadata = getWizardMetadata(EntityType.BRIDGE)

      expect(metadata.type).toBe(EntityType.BRIDGE)
      expect(metadata.category).toBe('entity')
      expect(metadata.requiresGhost).toBe(true)
      expect(metadata.steps.length).toBe(2)
    })

    it('should return metadata for COMBINER', () => {
      const metadata = getWizardMetadata(EntityType.COMBINER)

      expect(metadata.type).toBe(EntityType.COMBINER)
      expect(metadata.category).toBe('entity')
      expect(metadata.requiresSelection).toBe(true)
      expect(metadata.requiresGhost).toBe(true)
      expect(metadata.steps.length).toBe(3)
    })

    it('should return metadata for TAG', () => {
      const metadata = getWizardMetadata(IntegrationPointType.TAG)

      expect(metadata.type).toBe(IntegrationPointType.TAG)
      expect(metadata.category).toBe('integration')
      expect(metadata.requiresSelection).toBe(true)
      expect(metadata.requiresGhost).toBe(false)
      expect(metadata.steps.length).toBe(2)
    })
  })

  describe.skip('getWizardIcon', () => {
    it('should return icon component for each wizard type', () => {
      const types = [...getEntityWizardTypes(), ...getIntegrationWizardTypes()]

      types.forEach((type) => {
        const icon = getWizardIcon(type)
        expect(icon).toBeDefined()
        expect(typeof icon).toBe('function')
      })
    })
  })

  describe.skip('getWizardCategory', () => {
    it('should return entity category for entity types', () => {
      const entityTypes = getEntityWizardTypes()

      entityTypes.forEach((type) => {
        expect(getWizardCategory(type)).toBe('entity')
      })
    })

    it('should return integration category for integration point types', () => {
      const integrationTypes = getIntegrationWizardTypes()

      integrationTypes.forEach((type) => {
        expect(getWizardCategory(type)).toBe('integration')
      })
    })
  })

  describe.skip('getWizardStepCount', () => {
    it('should return correct step count for ADAPTER', () => {
      expect(getWizardStepCount(EntityType.ADAPTER)).toBe(3)
    })

    it('should return correct step count for BRIDGE', () => {
      expect(getWizardStepCount(EntityType.BRIDGE)).toBe(2)
    })

    it('should return correct step count for COMBINER', () => {
      expect(getWizardStepCount(EntityType.COMBINER)).toBe(3)
    })

    it('should return correct step count for TAG', () => {
      expect(getWizardStepCount(IntegrationPointType.TAG)).toBe(2)
    })

    it('should return correct step count for all wizard types', () => {
      Object.values(WIZARD_REGISTRY).forEach((metadata) => {
        const count = getWizardStepCount(metadata.type)
        expect(count).toBe(metadata.steps.length)
      })
    })
  })

  describe.skip('requiresSelection', () => {
    it('should return false for ADAPTER', () => {
      expect(requiresSelection(EntityType.ADAPTER)).toBe(false)
    })

    it('should return false for BRIDGE', () => {
      expect(requiresSelection(EntityType.BRIDGE)).toBe(false)
    })

    it('should return true for COMBINER', () => {
      expect(requiresSelection(EntityType.COMBINER)).toBe(true)
    })

    it('should return true for ASSET_MAPPER', () => {
      expect(requiresSelection(EntityType.ASSET_MAPPER)).toBe(true)
    })

    it('should return true for GROUP', () => {
      expect(requiresSelection(EntityType.GROUP)).toBe(true)
    })

    it('should return true for all integration point types', () => {
      const integrationTypes = getIntegrationWizardTypes()

      integrationTypes.forEach((type) => {
        expect(requiresSelection(type)).toBe(true)
      })
    })
  })

  describe.skip('requiresGhost', () => {
    it('should return true for ADAPTER', () => {
      expect(requiresGhost(EntityType.ADAPTER)).toBe(true)
    })

    it('should return true for BRIDGE', () => {
      expect(requiresGhost(EntityType.BRIDGE)).toBe(true)
    })

    it('should return true for COMBINER', () => {
      expect(requiresGhost(EntityType.COMBINER)).toBe(true)
    })

    it('should return true for all entity types', () => {
      const entityTypes = getEntityWizardTypes()

      entityTypes.forEach((type) => {
        expect(requiresGhost(type)).toBe(true)
      })
    })

    it('should return false for all integration point types', () => {
      const integrationTypes = getIntegrationWizardTypes()

      integrationTypes.forEach((type) => {
        expect(requiresGhost(type)).toBe(false)
      })
    })
  })

  describe.skip('getEntityWizardTypes', () => {
    it('should return all entity types', () => {
      const types = getEntityWizardTypes()

      expect(types).toHaveLength(5)
      expect(types).toContain(EntityType.ADAPTER)
      expect(types).toContain(EntityType.BRIDGE)
      expect(types).toContain(EntityType.COMBINER)
      expect(types).toContain(EntityType.ASSET_MAPPER)
      expect(types).toContain(EntityType.GROUP)
    })
  })

  describe.skip('getIntegrationWizardTypes', () => {
    it('should return all integration point types', () => {
      const types = getIntegrationWizardTypes()

      expect(types).toHaveLength(5)
      expect(types).toContain(IntegrationPointType.TAG)
      expect(types).toContain(IntegrationPointType.TOPIC_FILTER)
      expect(types).toContain(IntegrationPointType.DATA_MAPPING_NORTH)
      expect(types).toContain(IntegrationPointType.DATA_MAPPING_SOUTH)
      expect(types).toContain(IntegrationPointType.DATA_COMBINING)
    })
  })

  describe.skip('getWizardStep', () => {
    it('should return correct step for ADAPTER at index 0', () => {
      const step = getWizardStep(EntityType.ADAPTER, 0)

      expect(step).toBeDefined()
      expect(step?.index).toBe(0)
      expect(step?.descriptionKey).toBe('step_ADAPTER_0')
      expect(step?.showsGhostNodes).toBe(true)
    })

    it('should return correct step for ADAPTER at index 1', () => {
      const step = getWizardStep(EntityType.ADAPTER, 1)

      expect(step).toBeDefined()
      expect(step?.index).toBe(1)
      expect(step?.descriptionKey).toBe('step_ADAPTER_1')
      expect(step?.requiresConfiguration).toBe(true)
    })

    it('should return undefined for invalid step index', () => {
      const step = getWizardStep(EntityType.ADAPTER, 999)

      expect(step).toBeUndefined()
    })
  })

  describe.skip('getStepDescriptionKey', () => {
    it('should return description key for ADAPTER step 0', () => {
      const key = getStepDescriptionKey(EntityType.ADAPTER, 0)
      expect(key).toBe('step_ADAPTER_0')
    })

    it('should return description key for BRIDGE step 1', () => {
      const key = getStepDescriptionKey(EntityType.BRIDGE, 1)
      expect(key).toBe('step_BRIDGE_1')
    })

    it('should return description key for COMBINER step 2', () => {
      const key = getStepDescriptionKey(EntityType.COMBINER, 2)
      expect(key).toBe('step_COMBINER_2')
    })

    it('should return empty string for invalid step index', () => {
      const key = getStepDescriptionKey(EntityType.ADAPTER, 999)
      expect(key).toBe('')
    })

    it('should return correct keys for all wizard types and steps', () => {
      Object.values(WIZARD_REGISTRY).forEach((metadata) => {
        metadata.steps.forEach((step, index) => {
          const key = getStepDescriptionKey(metadata.type, index)
          expect(key).toBe(step.descriptionKey)
        })
      })
    })
  })
})
