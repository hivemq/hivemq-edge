import { describe, expect, it } from 'vitest'
import type { PulseStatus, Status } from '@/api/__generated__'
import { OperationalStatus, RuntimeStatus } from '../types/status.types'
import {
  createAdapterStatusModel,
  createBridgeStatusModel,
  createPulseStatusModel,
  createStaticStatusModel,
  mapAdapterStatusToRuntime,
  mapPulseStatusToRuntime,
} from './status-mapping.utils'

describe('status-mapping.utils', () => {
  describe('mapAdapterStatusToRuntime', () => {
    it('should return ACTIVE for STARTED + CONNECTED', () => {
      const status: Status = {
        runtime: 'STARTED' as Status['runtime'],
        connection: 'CONNECTED' as Status['connection'],
      }
      expect(mapAdapterStatusToRuntime(status)).toBe(RuntimeStatus.ACTIVE)
    })

    it('should return ACTIVE for STARTED + STATELESS', () => {
      const status: Status = {
        runtime: 'STARTED' as Status['runtime'],
        connection: 'STATELESS' as Status['connection'],
      }
      expect(mapAdapterStatusToRuntime(status)).toBe(RuntimeStatus.ACTIVE)
    })

    it('should return ERROR for STOPPED runtime', () => {
      const status: Status = {
        runtime: 'STOPPED' as Status['runtime'],
        connection: 'CONNECTED' as Status['connection'],
      }
      expect(mapAdapterStatusToRuntime(status)).toBe(RuntimeStatus.ERROR)
    })

    it('should return ERROR for ERROR connection', () => {
      const status: Status = {
        runtime: 'STARTED' as Status['runtime'],
        connection: 'ERROR' as Status['connection'],
      }
      expect(mapAdapterStatusToRuntime(status)).toBe(RuntimeStatus.ERROR)
    })

    it('should return INACTIVE for STARTED + DISCONNECTED', () => {
      const status: Status = {
        runtime: 'STARTED' as Status['runtime'],
        connection: 'DISCONNECTED' as Status['connection'],
      }
      expect(mapAdapterStatusToRuntime(status)).toBe(RuntimeStatus.INACTIVE)
    })

    it('should return INACTIVE for STARTED + UNKNOWN', () => {
      const status: Status = {
        runtime: 'STARTED' as Status['runtime'],
        connection: 'UNKNOWN' as Status['connection'],
      }
      expect(mapAdapterStatusToRuntime(status)).toBe(RuntimeStatus.INACTIVE)
    })

    it('should return INACTIVE for undefined status', () => {
      expect(mapAdapterStatusToRuntime(undefined)).toBe(RuntimeStatus.INACTIVE)
    })

    it('should return INACTIVE for missing runtime field', () => {
      const status: Status = {
        connection: 'CONNECTED' as Status['connection'],
      }
      expect(mapAdapterStatusToRuntime(status)).toBe(RuntimeStatus.INACTIVE)
    })
  })

  describe('mapPulseStatusToRuntime', () => {
    it('should return ACTIVE for ACTIVATED + CONNECTED', () => {
      const status: PulseStatus = {
        activation: 'ACTIVATED' as PulseStatus['activation'],
        runtime: 'CONNECTED' as PulseStatus['runtime'],
      }
      expect(mapPulseStatusToRuntime(status)).toBe(RuntimeStatus.ACTIVE)
    })

    it('should return ERROR for activation ERROR', () => {
      const status: PulseStatus = {
        activation: 'ERROR' as PulseStatus['activation'],
        runtime: 'CONNECTED' as PulseStatus['runtime'],
      }
      expect(mapPulseStatusToRuntime(status)).toBe(RuntimeStatus.ERROR)
    })

    it('should return ERROR for runtime ERROR', () => {
      const status: PulseStatus = {
        activation: 'ACTIVATED' as PulseStatus['activation'],
        runtime: 'ERROR' as PulseStatus['runtime'],
      }
      expect(mapPulseStatusToRuntime(status)).toBe(RuntimeStatus.ERROR)
    })

    it('should return INACTIVE for DEACTIVATED', () => {
      const status: PulseStatus = {
        activation: 'DEACTIVATED' as PulseStatus['activation'],
        runtime: 'CONNECTED' as PulseStatus['runtime'],
      }
      expect(mapPulseStatusToRuntime(status)).toBe(RuntimeStatus.INACTIVE)
    })

    it('should return INACTIVE for ACTIVATED + DISCONNECTED', () => {
      const status: PulseStatus = {
        activation: 'ACTIVATED' as PulseStatus['activation'],
        runtime: 'DISCONNECTED' as PulseStatus['runtime'],
      }
      expect(mapPulseStatusToRuntime(status)).toBe(RuntimeStatus.INACTIVE)
    })

    it('should return INACTIVE for undefined status', () => {
      expect(mapPulseStatusToRuntime(undefined)).toBe(RuntimeStatus.INACTIVE)
    })
  })

  describe('createAdapterStatusModel', () => {
    it('should create status model with ADAPTER source', () => {
      const status: Status = {
        runtime: 'STARTED' as Status['runtime'],
        connection: 'CONNECTED' as Status['connection'],
        id: 'adapter-1',
        lastActivity: '2025-10-25T10:00:00Z',
      }

      const model = createAdapterStatusModel(status)

      expect(model.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(model.operational).toBe(OperationalStatus.ACTIVE)
      expect(model.source).toBe('ADAPTER')
      expect(model.originalStatus).toBe(status)
      expect(model.lastUpdated).toBe('2025-10-25T10:00:00Z')
    })

    it('should create status model with BRIDGE source when type is bridge', () => {
      const status: Status = {
        runtime: 'STARTED' as Status['runtime'],
        connection: 'CONNECTED' as Status['connection'],
        type: 'bridge',
      }

      const model = createAdapterStatusModel(status)

      expect(model.source).toBe('BRIDGE')
    })

    it('should allow custom operational status', () => {
      const status: Status = {
        runtime: 'STARTED' as Status['runtime'],
        connection: 'CONNECTED' as Status['connection'],
      }

      const model = createAdapterStatusModel(status, OperationalStatus.INACTIVE)

      expect(model.operational).toBe(OperationalStatus.INACTIVE)
    })

    it('should handle undefined status', () => {
      const model = createAdapterStatusModel(undefined)

      expect(model.runtime).toBe(RuntimeStatus.INACTIVE)
      expect(model.operational).toBe(OperationalStatus.ACTIVE)
      expect(model.originalStatus).toBeUndefined()
      expect(model.lastUpdated).toBeDefined()
    })

    it('should use current timestamp when lastActivity is missing', () => {
      const status: Status = {
        runtime: 'STARTED' as Status['runtime'],
        connection: 'CONNECTED' as Status['connection'],
      }

      const beforeTime = new Date().toISOString()
      const model = createAdapterStatusModel(status)
      const afterTime = new Date().toISOString()

      expect(model.lastUpdated).toBeDefined()
      expect(model.lastUpdated! >= beforeTime).toBe(true)
      expect(model.lastUpdated! <= afterTime).toBe(true)
    })
  })

  describe('createBridgeStatusModel', () => {
    it('should be an alias for createAdapterStatusModel', () => {
      expect(createBridgeStatusModel).toBe(createAdapterStatusModel)
    })

    it('should create bridge status model', () => {
      const status: Status = {
        runtime: 'STARTED' as Status['runtime'],
        connection: 'CONNECTED' as Status['connection'],
        type: 'bridge',
      }

      const model = createBridgeStatusModel(status)

      expect(model.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(model.source).toBe('BRIDGE')
    })
  })

  describe('createPulseStatusModel', () => {
    it('should create status model with PULSE source', () => {
      const status: PulseStatus = {
        activation: 'ACTIVATED' as PulseStatus['activation'],
        runtime: 'CONNECTED' as PulseStatus['runtime'],
      }

      const model = createPulseStatusModel(status)

      expect(model.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(model.operational).toBe(OperationalStatus.ACTIVE)
      expect(model.source).toBe('PULSE')
      expect(model.originalStatus).toBe(status)
      expect(model.lastUpdated).toBeDefined()
    })

    it('should allow custom operational status', () => {
      const status: PulseStatus = {
        activation: 'ACTIVATED' as PulseStatus['activation'],
        runtime: 'CONNECTED' as PulseStatus['runtime'],
      }

      const model = createPulseStatusModel(status, OperationalStatus.ERROR)

      expect(model.operational).toBe(OperationalStatus.ERROR)
    })

    it('should handle undefined status', () => {
      const model = createPulseStatusModel(undefined)

      expect(model.runtime).toBe(RuntimeStatus.INACTIVE)
      expect(model.operational).toBe(OperationalStatus.ACTIVE)
      expect(model.originalStatus).toBeUndefined()
    })
  })

  describe('createStaticStatusModel', () => {
    it('should create status model with STATIC source', () => {
      const model = createStaticStatusModel()

      expect(model.runtime).toBe(RuntimeStatus.INACTIVE)
      expect(model.operational).toBe(OperationalStatus.INACTIVE)
      expect(model.source).toBe('STATIC')
      expect(model.lastUpdated).toBeDefined()
      expect(model.originalStatus).toBeUndefined()
    })

    it('should allow custom runtime status', () => {
      const model = createStaticStatusModel(RuntimeStatus.ACTIVE)

      expect(model.runtime).toBe(RuntimeStatus.ACTIVE)
    })

    it('should allow custom operational status', () => {
      const model = createStaticStatusModel(RuntimeStatus.INACTIVE, OperationalStatus.ACTIVE)

      expect(model.operational).toBe(OperationalStatus.ACTIVE)
    })

    it('should allow both custom statuses', () => {
      const model = createStaticStatusModel(RuntimeStatus.ERROR, OperationalStatus.ERROR)

      expect(model.runtime).toBe(RuntimeStatus.ERROR)
      expect(model.operational).toBe(OperationalStatus.ERROR)
    })
  })
})
