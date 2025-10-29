import { describe, expect, it } from 'vitest'
import type { PulseStatus, Status } from '@/api/__generated__'
import {
  hasStatusModel,
  isAdapterBridgeStatus,
  isPulseStatus,
  OperationalStatus,
  RuntimeStatus,
  type NodeStatusModel,
} from './status.types'

describe('status.types', () => {
  describe('RuntimeStatus', () => {
    it('should have correct enum values', () => {
      expect(RuntimeStatus.ACTIVE).toBe('ACTIVE')
      expect(RuntimeStatus.INACTIVE).toBe('INACTIVE')
      expect(RuntimeStatus.ERROR).toBe('ERROR')
    })
  })

  describe('OperationalStatus', () => {
    it('should have correct enum values', () => {
      expect(OperationalStatus.ACTIVE).toBe('ACTIVE')
      expect(OperationalStatus.INACTIVE).toBe('INACTIVE')
      expect(OperationalStatus.ERROR).toBe('ERROR')
    })
  })

  describe('hasStatusModel', () => {
    it('should return true for object with statusModel', () => {
      const data = {
        statusModel: {
          runtime: RuntimeStatus.ACTIVE,
          operational: OperationalStatus.ACTIVE,
          source: 'ADAPTER' as const,
        } satisfies NodeStatusModel,
      }
      expect(hasStatusModel(data)).toBe(true)
    })

    it('should return false for object without statusModel', () => {
      const data = { someOtherProp: 'value' }
      expect(hasStatusModel(data)).toBe(false)
    })

    it('should return false for null', () => {
      expect(hasStatusModel(null)).toBe(false)
    })

    it('should return false for undefined', () => {
      expect(hasStatusModel(undefined)).toBe(false)
    })

    it('should return false for primitive values', () => {
      expect(hasStatusModel('string')).toBe(false)
      expect(hasStatusModel(123)).toBe(false)
      expect(hasStatusModel(true)).toBe(false)
    })
  })

  describe('isAdapterBridgeStatus', () => {
    it('should return true for Status type', () => {
      const status: Status = {
        connection: 'CONNECTED' as Status['connection'],
        runtime: 'STARTED' as Status['runtime'],
      }
      expect(isAdapterBridgeStatus(status)).toBe(true)
    })

    it('should return false for PulseStatus type', () => {
      const status: PulseStatus = {
        activation: 'ACTIVATED' as PulseStatus['activation'],
        runtime: 'CONNECTED' as PulseStatus['runtime'],
      }
      expect(isAdapterBridgeStatus(status)).toBe(false)
    })

    it('should return false for undefined', () => {
      expect(isAdapterBridgeStatus(undefined)).toBe(false)
    })
  })

  describe('isPulseStatus', () => {
    it('should return true for PulseStatus type', () => {
      const status: PulseStatus = {
        activation: 'ACTIVATED' as PulseStatus['activation'],
        runtime: 'CONNECTED' as PulseStatus['runtime'],
      }
      expect(isPulseStatus(status)).toBe(true)
    })

    it('should return false for Status type', () => {
      const status: Status = {
        connection: 'CONNECTED' as Status['connection'],
        runtime: 'STARTED' as Status['runtime'],
      }
      expect(isPulseStatus(status)).toBe(false)
    })

    it('should return false for undefined', () => {
      expect(isPulseStatus(undefined)).toBe(false)
    })
  })

  describe('NodeStatusModel', () => {
    it('should allow creation with all required fields', () => {
      const statusModel: NodeStatusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'ADAPTER',
      }
      expect(statusModel.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(statusModel.operational).toBe(OperationalStatus.ACTIVE)
      expect(statusModel.source).toBe('ADAPTER')
    })

    it('should allow optional fields', () => {
      const statusModel: NodeStatusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.INACTIVE,
        source: 'DERIVED',
        originalStatus: {
          connection: 'CONNECTED' as Status['connection'],
          runtime: 'STARTED' as Status['runtime'],
        },
        lastUpdated: '2025-10-25T10:00:00Z',
      }
      expect(statusModel.originalStatus).toBeDefined()
      expect(statusModel.lastUpdated).toBe('2025-10-25T10:00:00Z')
    })
  })
})
