import { describe, expect, it } from 'vitest'
import { MOCK_THEME } from '@/__test-utils__/react-flow/utils.ts'

import { RuntimeStatus, OperationalStatus } from '@/modules/Workspace/types/status.types'
import { getStatusColor } from '@/modules/Workspace/utils/status-utils'

describe('getStatusColor', () => {
  it('should return green for ACTIVE runtime status', () => {
    const statusModel = {
      runtime: RuntimeStatus.ACTIVE,
      operational: OperationalStatus.ACTIVE,
      source: 'DERIVED' as const,
    }

    const color = getStatusColor(MOCK_THEME, statusModel)
    expect(color).toBe(MOCK_THEME.colors.status.connected[500])
  })

  it('should return red for ERROR runtime status', () => {
    const statusModel = {
      runtime: RuntimeStatus.ERROR,
      operational: OperationalStatus.ACTIVE,
      source: 'DERIVED' as const,
    }

    const color = getStatusColor(MOCK_THEME, statusModel)
    expect(color).toBe(MOCK_THEME.colors.status.error[500])
  })

  it('should return yellow/gray for INACTIVE runtime status', () => {
    const statusModel = {
      runtime: RuntimeStatus.INACTIVE,
      operational: OperationalStatus.INACTIVE,
      source: 'DERIVED' as const,
    }

    const color = getStatusColor(MOCK_THEME, statusModel)
    expect(color).toBe(MOCK_THEME.colors.status.disconnected[500])
  })

  it('should return disconnected color when statusModel is undefined', () => {
    const color = getStatusColor(MOCK_THEME, undefined)
    expect(color).toBe(MOCK_THEME.colors.status.disconnected[500])
  })

  it('should return fallback color when theme colors are not available', () => {
    const emptyTheme = { colors: {} }
    const color = getStatusColor(emptyTheme, undefined)
    expect(color).toBe('#cbd5e0') // fallback gray
  })

  it('should work regardless of operational status (only runtime matters)', () => {
    const activeRuntime = {
      runtime: RuntimeStatus.ACTIVE,
      operational: OperationalStatus.INACTIVE, // Different operational status
      source: 'DERIVED' as const,
    }

    const color = getStatusColor(MOCK_THEME, activeRuntime)
    // Should still be green because runtime is ACTIVE
    expect(color).toBe(MOCK_THEME.colors.status.connected[500])
  })
})
