import { describe, it, expect, vi, beforeEach } from 'vitest'
import { configureJSON } from './json.config'
import type { MonacoInstance } from '../types'

describe('JSON Config', () => {
  let mockMonaco: MonacoInstance

  beforeEach(() => {
    mockMonaco = {
      languages: {
        json: {
          jsonDefaults: {
            setDiagnosticsOptions: vi.fn(),
            setModeConfiguration: vi.fn(),
          },
        },
      },
    } as unknown as MonacoInstance
  })

  it('should configure JSON diagnostics with validation enabled', () => {
    configureJSON(mockMonaco)

    expect(mockMonaco.languages.json.jsonDefaults.setDiagnosticsOptions).toHaveBeenCalled()
    expect(mockMonaco.languages.json.jsonDefaults.setModeConfiguration).toHaveBeenCalled()

    const call = (mockMonaco.languages.json.jsonDefaults.setDiagnosticsOptions as ReturnType<typeof vi.fn>).mock
      .calls[0][0]
    expect(call.validate).toBe(true)
    expect(call.schemas).toBeInstanceOf(Array)
    expect(call.schemas.length).toBeGreaterThan(0)
  })

  it('should include JSON Schema meta-schema', () => {
    configureJSON(mockMonaco)

    const call = (mockMonaco.languages.json.jsonDefaults.setDiagnosticsOptions as ReturnType<typeof vi.fn>).mock
      .calls[0][0]
    expect(call.schemas).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          uri: 'http://json-schema.org/draft-07/schema#',
        }),
      ])
    )
  })
})
