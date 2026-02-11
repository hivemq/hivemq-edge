import { describe, expect, it } from 'vitest'

import { BehaviorPolicyType } from '@datahub/types.ts'

import type { ModelMetadata } from './behaviorModelMetadata.utils.ts'
import { extractModelMetadata } from './behaviorModelMetadata.utils.ts'

describe('extractModelMetadata', () => {
  it('should return an array of model metadata', () => {
    const result = extractModelMetadata()

    expect(result).toBeInstanceOf(Array)
    expect(result.length).toBe(3)
  })

  it('should return metadata for all three behavior models', () => {
    const result = extractModelMetadata()
    const modelIds = result.map((model) => model.id)

    expect(modelIds).toContain(BehaviorPolicyType.MQTT_EVENT)
    expect(modelIds).toContain(BehaviorPolicyType.PUBLISH_DUPLICATE)
    expect(modelIds).toContain(BehaviorPolicyType.PUBLISH_QUOTA)
  })

  it('should include all required properties for each model', () => {
    const result = extractModelMetadata()

    result.forEach((model: ModelMetadata) => {
      expect(model).toHaveProperty('id')
      expect(model).toHaveProperty('title')
      expect(model).toHaveProperty('description')
      expect(model).toHaveProperty('requiresArguments')
      expect(model).toHaveProperty('stateCount')
      expect(model).toHaveProperty('transitionCount')
      expect(model).toHaveProperty('hasSuccessState')
      expect(model).toHaveProperty('hasFailedState')

      // Validate types
      expect(typeof model.id).toBe('string')
      expect(typeof model.title).toBe('string')
      expect(typeof model.description).toBe('string')
      expect(typeof model.requiresArguments).toBe('boolean')
      expect(typeof model.stateCount).toBe('number')
      expect(typeof model.transitionCount).toBe('number')
      expect(typeof model.hasSuccessState).toBe('boolean')
      expect(typeof model.hasFailedState).toBe('boolean')
    })
  })

  describe('Mqtt.events model', () => {
    it('should extract correct metadata for Mqtt.events', () => {
      const result = extractModelMetadata()
      const mqttEventsModel = result.find((model) => model.id === BehaviorPolicyType.MQTT_EVENT)

      expect(mqttEventsModel).toBeDefined()
      expect(mqttEventsModel).toEqual({
        id: BehaviorPolicyType.MQTT_EVENT,
        title: 'MQTT - Events',
        description:
          'The MQTT - Events behavior model allows you to intercept specific MQTT packets for further actions. The model itself does not enforce any particular behavior and is very useful in debugging scenarios. This model does not require any arguments.',
        requiresArguments: false,
        stateCount: 3,
        transitionCount: 5,
        hasSuccessState: true,
        hasFailedState: false,
      })
    })

    it('should identify that Mqtt.events does not require arguments', () => {
      const result = extractModelMetadata()
      const mqttEventsModel = result.find((model) => model.id === BehaviorPolicyType.MQTT_EVENT)

      expect(mqttEventsModel?.requiresArguments).toBe(false)
    })

    it('should identify that Mqtt.events has SUCCESS but not FAILED state', () => {
      const result = extractModelMetadata()
      const mqttEventsModel = result.find((model) => model.id === BehaviorPolicyType.MQTT_EVENT)

      expect(mqttEventsModel?.hasSuccessState).toBe(true)
      expect(mqttEventsModel?.hasFailedState).toBe(false)
    })
  })

  describe('Publish.duplicate model', () => {
    it('should extract correct metadata for Publish.duplicate', () => {
      const result = extractModelMetadata()
      const publishDuplicateModel = result.find((model) => model.id === BehaviorPolicyType.PUBLISH_DUPLICATE)

      expect(publishDuplicateModel).toBeDefined()
      expect(publishDuplicateModel).toEqual({
        id: BehaviorPolicyType.PUBLISH_DUPLICATE,
        title: 'Publish - Duplicate',
        description:
          'The Publish - Duplicate model identifies consecutive identical client messages to prevent unnecessary resource consumption. This model does not require any arguments.',
        requiresArguments: false,
        stateCount: 6,
        transitionCount: 10,
        hasSuccessState: true,
        hasFailedState: true,
      })
    })

    it('should identify that Publish.duplicate does not require arguments', () => {
      const result = extractModelMetadata()
      const publishDuplicateModel = result.find((model) => model.id === BehaviorPolicyType.PUBLISH_DUPLICATE)

      expect(publishDuplicateModel?.requiresArguments).toBe(false)
    })

    it('should identify that Publish.duplicate has both SUCCESS and FAILED states', () => {
      const result = extractModelMetadata()
      const publishDuplicateModel = result.find((model) => model.id === BehaviorPolicyType.PUBLISH_DUPLICATE)

      expect(publishDuplicateModel?.hasSuccessState).toBe(true)
      expect(publishDuplicateModel?.hasFailedState).toBe(true)
    })
  })

  describe('Publish.quota model', () => {
    it('should extract correct metadata for Publish.quota', () => {
      const result = extractModelMetadata()
      const publishQuotaModel = result.find((model) => model.id === BehaviorPolicyType.PUBLISH_QUOTA)

      expect(publishQuotaModel).toBeDefined()
      expect(publishQuotaModel).toEqual({
        id: BehaviorPolicyType.PUBLISH_QUOTA,
        title: 'Publish - Quota',
        description:
          'The Publish - Quota model tracks the number of MQTT PUBLISH messages a client sends after a client connects to the broker to identify unusual behavior. When you configure a publish-quota model, at least one of the available arguments must be present. Data Hub uses the default value for the missing parameter.',
        requiresArguments: true,
        stateCount: 5,
        transitionCount: 7,
        hasSuccessState: true,
        hasFailedState: true,
      })
    })

    it('should identify that Publish.quota requires arguments', () => {
      const result = extractModelMetadata()
      const publishQuotaModel = result.find((model) => model.id === BehaviorPolicyType.PUBLISH_QUOTA)

      expect(publishQuotaModel?.requiresArguments).toBe(true)
    })

    it('should identify that Publish.quota has both SUCCESS and FAILED states', () => {
      const result = extractModelMetadata()
      const publishQuotaModel = result.find((model) => model.id === BehaviorPolicyType.PUBLISH_QUOTA)

      expect(publishQuotaModel?.hasSuccessState).toBe(true)
      expect(publishQuotaModel?.hasFailedState).toBe(true)
    })
  })

  describe('state and transition counts', () => {
    it('should correctly count states for each model', () => {
      const result = extractModelMetadata()

      const mqttEventsModel = result.find((model) => model.id === BehaviorPolicyType.MQTT_EVENT)
      const publishDuplicateModel = result.find((model) => model.id === BehaviorPolicyType.PUBLISH_DUPLICATE)
      const publishQuotaModel = result.find((model) => model.id === BehaviorPolicyType.PUBLISH_QUOTA)

      expect(mqttEventsModel?.stateCount).toBe(3)
      expect(publishDuplicateModel?.stateCount).toBe(6)
      expect(publishQuotaModel?.stateCount).toBe(5)
    })

    it('should correctly count transitions for each model', () => {
      const result = extractModelMetadata()

      const mqttEventsModel = result.find((model) => model.id === BehaviorPolicyType.MQTT_EVENT)
      const publishDuplicateModel = result.find((model) => model.id === BehaviorPolicyType.PUBLISH_DUPLICATE)
      const publishQuotaModel = result.find((model) => model.id === BehaviorPolicyType.PUBLISH_QUOTA)

      expect(mqttEventsModel?.transitionCount).toBe(5)
      expect(publishDuplicateModel?.transitionCount).toBe(10)
      expect(publishQuotaModel?.transitionCount).toBe(7)
    })

    it('should return counts greater than zero for all models', () => {
      const result = extractModelMetadata()

      result.forEach((model) => {
        expect(model.stateCount).toBeGreaterThan(0)
        expect(model.transitionCount).toBeGreaterThan(0)
      })
    })
  })

  describe('terminal states', () => {
    it('should identify SUCCESS states correctly', () => {
      const result = extractModelMetadata()

      // All three models have SUCCESS states
      result.forEach((model) => {
        expect(model.hasSuccessState).toBe(true)
      })
    })

    it('should identify FAILED states correctly', () => {
      const result = extractModelMetadata()

      const mqttEventsModel = result.find((model) => model.id === BehaviorPolicyType.MQTT_EVENT)
      const publishDuplicateModel = result.find((model) => model.id === BehaviorPolicyType.PUBLISH_DUPLICATE)
      const publishQuotaModel = result.find((model) => model.id === BehaviorPolicyType.PUBLISH_QUOTA)

      // Only Mqtt.events does NOT have FAILED state
      expect(mqttEventsModel?.hasFailedState).toBe(false)
      expect(publishDuplicateModel?.hasFailedState).toBe(true)
      expect(publishQuotaModel?.hasFailedState).toBe(true)
    })
  })

  describe('titles and descriptions', () => {
    it('should have non-empty titles for all models', () => {
      const result = extractModelMetadata()

      result.forEach((model) => {
        expect(model.title).toBeTruthy()
        expect(model.title.length).toBeGreaterThan(0)
      })
    })

    it('should have non-empty descriptions for all models', () => {
      const result = extractModelMetadata()

      result.forEach((model) => {
        expect(model.description).toBeTruthy()
        expect(model.description.length).toBeGreaterThan(0)
      })
    })

    it('should use human-readable titles from arguments spec', () => {
      const result = extractModelMetadata()

      const mqttEventsModel = result.find((model) => model.id === BehaviorPolicyType.MQTT_EVENT)
      const publishDuplicateModel = result.find((model) => model.id === BehaviorPolicyType.PUBLISH_DUPLICATE)
      const publishQuotaModel = result.find((model) => model.id === BehaviorPolicyType.PUBLISH_QUOTA)

      expect(mqttEventsModel?.title).toBe('MQTT - Events')
      expect(publishDuplicateModel?.title).toBe('Publish - Duplicate')
      expect(publishQuotaModel?.title).toBe('Publish - Quota')
    })
  })

  describe('argument requirements', () => {
    it('should identify models that require arguments', () => {
      const result = extractModelMetadata()
      const modelsRequiringArguments = result.filter((model) => model.requiresArguments)

      expect(modelsRequiringArguments).toHaveLength(1)
      expect(modelsRequiringArguments[0].id).toBe(BehaviorPolicyType.PUBLISH_QUOTA)
    })

    it('should identify models that do not require arguments', () => {
      const result = extractModelMetadata()
      const modelsNotRequiringArguments = result.filter((model) => !model.requiresArguments)

      expect(modelsNotRequiringArguments).toHaveLength(2)
      const ids = modelsNotRequiringArguments.map((model) => model.id)
      expect(ids).toContain(BehaviorPolicyType.MQTT_EVENT)
      expect(ids).toContain(BehaviorPolicyType.PUBLISH_DUPLICATE)
    })
  })
})
