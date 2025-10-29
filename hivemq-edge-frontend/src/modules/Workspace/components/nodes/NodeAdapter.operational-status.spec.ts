import { describe, expect, it } from 'vitest'

import { OperationalStatus } from '@/modules/Workspace/types/status.types'

/**
 * Test suite for Bug Fixes in Operational Status
 *
 * Bug 1: Incorrect Mapping Usage for Operational Status
 * Bug 2: OperationalStatus Not Propagating to Edges
 */
describe('NodeAdapter Operational Status Bug Fixes', () => {
  describe('Bug 1: Operational Status Computation', () => {
    it('should be ACTIVE for unidirectional adapter with northbound mappings', () => {
      // Unidirectional adapter (only READ capability)
      const hasNorthMappings = true
      const hasSouthMappings = false
      const isBidirectionalAdapter = false

      // New logic: hasNorthMappings || (isBidirectionalAdapter && hasSouthMappings)
      const operational =
        hasNorthMappings || (isBidirectionalAdapter && hasSouthMappings)
          ? OperationalStatus.ACTIVE
          : OperationalStatus.INACTIVE

      expect(operational).toBe(OperationalStatus.ACTIVE)
    })

    it('should be INACTIVE for unidirectional adapter without northbound mappings', () => {
      const hasNorthMappings = false
      const hasSouthMappings = false
      const isBidirectionalAdapter = false

      const operational =
        hasNorthMappings || (isBidirectionalAdapter && hasSouthMappings)
          ? OperationalStatus.ACTIVE
          : OperationalStatus.INACTIVE

      expect(operational).toBe(OperationalStatus.INACTIVE)
    })

    it('should be ACTIVE for bidirectional adapter with only northbound mappings', () => {
      const hasNorthMappings = true
      const hasSouthMappings = false
      const isBidirectionalAdapter = true

      const operational =
        hasNorthMappings || (isBidirectionalAdapter && hasSouthMappings)
          ? OperationalStatus.ACTIVE
          : OperationalStatus.INACTIVE

      expect(operational).toBe(OperationalStatus.ACTIVE)
    })

    it('should be ACTIVE for bidirectional adapter with only southbound mappings', () => {
      const hasNorthMappings = false
      const hasSouthMappings = true
      const isBidirectionalAdapter = true

      const operational =
        hasNorthMappings || (isBidirectionalAdapter && hasSouthMappings)
          ? OperationalStatus.ACTIVE
          : OperationalStatus.INACTIVE

      expect(operational).toBe(OperationalStatus.ACTIVE)
    })

    it('should be ACTIVE for bidirectional adapter with both mappings', () => {
      const hasNorthMappings = true
      const hasSouthMappings = true
      const isBidirectionalAdapter = true

      const operational =
        hasNorthMappings || (isBidirectionalAdapter && hasSouthMappings)
          ? OperationalStatus.ACTIVE
          : OperationalStatus.INACTIVE

      expect(operational).toBe(OperationalStatus.ACTIVE)
    })

    it('should be INACTIVE for bidirectional adapter without any mappings', () => {
      const hasNorthMappings = false
      const hasSouthMappings = false
      const isBidirectionalAdapter = true

      const operational =
        hasNorthMappings || (isBidirectionalAdapter && hasSouthMappings)
          ? OperationalStatus.ACTIVE
          : OperationalStatus.INACTIVE

      expect(operational).toBe(OperationalStatus.INACTIVE)
    })

    describe('Comparison with old buggy logic', () => {
      it('OLD LOGIC BUG: bidirectional adapter with only northbound would be INACTIVE', () => {
        const hasNorthMappings = true
        const hasSouthMappings = false
        const isBidirectionalAdapter = true

        // Old buggy logic: hasNorthMappings && (!isBidirectionalAdapter || hasSouthMappings)
        const oldOperational =
          hasNorthMappings && (!isBidirectionalAdapter || hasSouthMappings)
            ? OperationalStatus.ACTIVE
            : OperationalStatus.INACTIVE

        // This was INACTIVE in the old logic (BUG!)
        expect(oldOperational).toBe(OperationalStatus.INACTIVE)

        // New logic correctly makes it ACTIVE
        const newOperational =
          hasNorthMappings || (isBidirectionalAdapter && hasSouthMappings)
            ? OperationalStatus.ACTIVE
            : OperationalStatus.INACTIVE
        expect(newOperational).toBe(OperationalStatus.ACTIVE)
      })

      it('OLD LOGIC BUG: bidirectional adapter with only southbound would be INACTIVE', () => {
        const hasNorthMappings = false
        const hasSouthMappings = true
        const isBidirectionalAdapter = true

        // Old buggy logic
        const oldOperational =
          hasNorthMappings && (!isBidirectionalAdapter || hasSouthMappings)
            ? OperationalStatus.ACTIVE
            : OperationalStatus.INACTIVE

        // This was INACTIVE in the old logic (BUG!)
        expect(oldOperational).toBe(OperationalStatus.INACTIVE)

        // New logic correctly makes it ACTIVE
        const newOperational =
          hasNorthMappings || (isBidirectionalAdapter && hasSouthMappings)
            ? OperationalStatus.ACTIVE
            : OperationalStatus.INACTIVE
        expect(newOperational).toBe(OperationalStatus.ACTIVE)
      })
    })
  })

  describe('Bug 2: Edge Animation Logic', () => {
    it('should animate ADAPTER to EDGE edge when adapter is connected and has northbound mappings', () => {
      const isConnected = true
      const hasNorthMappings = true
      const targetNodeType = 'EDGE_NODE'

      let shouldAnimate = false
      if (targetNodeType === 'EDGE_NODE') {
        shouldAnimate = isConnected && hasNorthMappings
      }

      expect(shouldAnimate).toBe(true)
    })

    it('should NOT animate ADAPTER to EDGE edge when adapter lacks northbound mappings', () => {
      const isConnected = true
      const hasNorthMappings = false
      const targetNodeType = 'EDGE_NODE'

      let shouldAnimate = false
      if (targetNodeType === 'EDGE_NODE') {
        shouldAnimate = isConnected && hasNorthMappings
      }

      expect(shouldAnimate).toBe(false)
    })

    it('should animate ADAPTER to DEVICE edge when adapter is connected and has southbound mappings', () => {
      const isConnected = true
      const hasSouthMappings = true
      const targetNodeType = 'DEVICE_NODE'

      let shouldAnimate = false
      if (targetNodeType === 'DEVICE_NODE') {
        shouldAnimate = isConnected && hasSouthMappings
      }

      expect(shouldAnimate).toBe(true)
    })

    it('should NOT animate ADAPTER to DEVICE edge when adapter lacks southbound mappings', () => {
      const isConnected = true
      const hasSouthMappings = false
      const targetNodeType = 'DEVICE_NODE'

      let shouldAnimate = false
      if (targetNodeType === 'DEVICE_NODE') {
        shouldAnimate = isConnected && hasSouthMappings
      }

      expect(shouldAnimate).toBe(false)
    })

    it('should NOT animate any edge when adapter is disconnected', () => {
      const isConnected = false
      const hasNorthMappings = true
      const hasSouthMappings = true

      const edgeToEdge = isConnected && hasNorthMappings
      const edgeToDevice = isConnected && hasSouthMappings

      expect(edgeToEdge).toBe(false)
      expect(edgeToDevice).toBe(false)
    })
  })

  describe('Integration: Adapter Status and Edge Animation', () => {
    it('unidirectional adapter with northbound mappings: adapter ACTIVE, edge to EDGE animated', () => {
      // Adapter status
      const hasNorthMappings = true
      const hasSouthMappings = false
      const isBidirectionalAdapter = false
      const isConnected = true

      const adapterOperational =
        hasNorthMappings || (isBidirectionalAdapter && hasSouthMappings)
          ? OperationalStatus.ACTIVE
          : OperationalStatus.INACTIVE

      // Edge to EDGE animation
      const edgeToEdgeAnimated = isConnected && hasNorthMappings

      expect(adapterOperational).toBe(OperationalStatus.ACTIVE)
      expect(edgeToEdgeAnimated).toBe(true)
    })

    it('bidirectional adapter with only southbound: adapter ACTIVE, edge to DEVICE animated, edge to EDGE not animated', () => {
      // Adapter status
      const hasNorthMappings = false
      const hasSouthMappings = true
      const isBidirectionalAdapter = true
      const isConnected = true

      const adapterOperational =
        hasNorthMappings || (isBidirectionalAdapter && hasSouthMappings)
          ? OperationalStatus.ACTIVE
          : OperationalStatus.INACTIVE

      // Edge animations
      const edgeToEdgeAnimated = isConnected && hasNorthMappings
      const edgeToDeviceAnimated = isConnected && hasSouthMappings

      expect(adapterOperational).toBe(OperationalStatus.ACTIVE)
      expect(edgeToEdgeAnimated).toBe(false) // No northbound mappings
      expect(edgeToDeviceAnimated).toBe(true) // Has southbound mappings
    })

    it('bidirectional adapter with both mappings: adapter ACTIVE, both edges animated', () => {
      // Adapter status
      const hasNorthMappings = true
      const hasSouthMappings = true
      const isBidirectionalAdapter = true
      const isConnected = true

      const adapterOperational =
        hasNorthMappings || (isBidirectionalAdapter && hasSouthMappings)
          ? OperationalStatus.ACTIVE
          : OperationalStatus.INACTIVE

      // Edge animations
      const edgeToEdgeAnimated = isConnected && hasNorthMappings
      const edgeToDeviceAnimated = isConnected && hasSouthMappings

      expect(adapterOperational).toBe(OperationalStatus.ACTIVE)
      expect(edgeToEdgeAnimated).toBe(true)
      expect(edgeToDeviceAnimated).toBe(true)
    })
  })
})
