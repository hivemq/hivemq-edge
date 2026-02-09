import { describe, expect, it } from 'vitest'
import type { ColorMode, WithCSSVar } from '@chakra-ui/react'
import type { Dict } from '@chakra-ui/utils'
import type { FsmState, FsmTransition } from '@datahub/types.ts'

import { resolveColor, getBadgeColors, getStateClass, getTransitionLabel } from './MermaidRenderer.utils'

describe('resolveColor', () => {
  const mockTheme = {
    colors: {
      blue: {
        100: '#EBF8FF',
        200: '#BEE3F8',
        300: '#90CDF4',
        800: '#2C5282',
        _light: 'colors.blue.200',
        _dark: 'colors.blue.300',
      },
      gray: {
        100: '#F7FAFC',
        200: '#EDF2F7',
        300: '#E2E8F0',
        800: '#1A202C',
        900: '#171923',
      },
      green: {
        100: '#F0FFF4',
        200: '#C6F6D5',
        300: '#9AE6B4',
        800: '#22543D',
        900: '#1C4532',
      },
      red: {
        100: '#FFF5F5',
        200: '#FED7D7',
        300: '#FC8181',
        800: '#9B2C2C',
        900: '#742A2A',
      },
    },
  } as unknown as WithCSSVar<Dict>

  describe('simple color resolution', () => {
    it('should resolve a simple color path', () => {
      const result = resolveColor('blue.100', mockTheme, 'light')
      expect(result).toBe('#EBF8FF')
    })

    it('should resolve nested color paths', () => {
      const result = resolveColor('gray.300', mockTheme, 'light')
      expect(result).toBe('#E2E8F0')
    })

    it('should return fallback color for non-existent path', () => {
      const result = resolveColor('nonexistent.color', mockTheme, 'light')
      expect(result).toBe('#000000')
    })

    it('should return fallback color for invalid path', () => {
      const result = resolveColor('blue.nonexistent', mockTheme, 'light')
      expect(result).toBe('#000000')
    })
  })

  describe('color mode resolution', () => {
    it('should resolve _light color in light mode', () => {
      const result = resolveColor('blue', mockTheme, 'light')
      expect(result).toBe('#BEE3F8') // Should resolve to blue.200
    })

    it('should resolve _dark color in dark mode', () => {
      const result = resolveColor('blue', mockTheme, 'dark')
      expect(result).toBe('#90CDF4') // Should resolve to blue.300
    })

    it('should handle recursive color resolution', () => {
      const themeWithRecursive = {
        colors: {
          primary: {
            _light: 'colors.blue.200',
            _dark: 'colors.blue.300',
          },
          blue: {
            200: '#BEE3F8',
            300: '#90CDF4',
          },
        },
      } as unknown as WithCSSVar<Dict>
      const result = resolveColor('primary', themeWithRecursive, 'light')
      expect(result).toBe('#BEE3F8')
    })
  })

  describe('edge cases', () => {
    it('should handle empty color path', () => {
      const result = resolveColor('', mockTheme, 'light')
      expect(result).toBe('#000000')
    })

    it('should handle undefined in color object', () => {
      const themeWithUndefined = {
        colors: {
          test: undefined,
        },
      } as unknown as WithCSSVar<Dict>
      const result = resolveColor('test', themeWithUndefined, 'light')
      expect(result).toBe('#000000')
    })

    it('should return fallback when color value is not a string', () => {
      const themeWithNonString = {
        colors: {
          test: {
            invalid: 123,
          },
        },
      } as unknown as WithCSSVar<Dict>
      const result = resolveColor('test.invalid', themeWithNonString, 'light')
      expect(result).toBe('#000000')
    })
  })
})

describe('getBadgeColors', () => {
  const mockTheme = {
    colors: {
      blue: {
        100: '#EBF8FF',
        200: '#BEE3F8',
        300: '#90CDF4',
        800: '#2C5282',
        900: '#1A365D',
      },
      gray: {
        100: '#F7FAFC',
        200: '#EDF2F7',
        300: '#E2E8F0',
        800: '#1A202C',
        900: '#171923',
      },
      green: {
        100: '#F0FFF4',
        200: '#C6F6D5',
        300: '#9AE6B4',
        800: '#22543D',
        900: '#1C4532',
      },
      red: {
        100: '#FFF5F5',
        200: '#FED7D7',
        300: '#FC8181',
        800: '#9B2C2C',
        900: '#742A2A',
      },
    },
  } as unknown as WithCSSVar<Dict>

  describe('light mode', () => {
    const colorMode: ColorMode = 'light'

    it('should return blue badge colors', () => {
      const colors = getBadgeColors('blue', mockTheme, colorMode)
      expect(colors).toEqual({
        fill: '#EBF8FF',
        stroke: '#BEE3F8',
        text: '#2C5282',
      })
    })

    it('should return gray badge colors', () => {
      const colors = getBadgeColors('gray', mockTheme, colorMode)
      expect(colors).toEqual({
        fill: '#F7FAFC',
        stroke: '#EDF2F7',
        text: '#1A202C',
      })
    })

    it('should return green badge colors', () => {
      const colors = getBadgeColors('green', mockTheme, colorMode)
      expect(colors).toEqual({
        fill: '#F0FFF4',
        stroke: '#C6F6D5',
        text: '#22543D',
      })
    })

    it('should return red badge colors', () => {
      const colors = getBadgeColors('red', mockTheme, colorMode)
      expect(colors).toEqual({
        fill: '#FFF5F5',
        stroke: '#FED7D7',
        text: '#9B2C2C',
      })
    })

    it('should return default gray badge colors for unknown color scheme', () => {
      const colors = getBadgeColors('unknown', mockTheme, colorMode)
      expect(colors).toEqual({
        fill: '#F7FAFC',
        stroke: '#EDF2F7',
        text: '#1A202C',
      })
    })
  })

  describe('dark mode', () => {
    const colorMode: ColorMode = 'dark'

    it('should return blue badge colors', () => {
      const colors = getBadgeColors('blue', mockTheme, colorMode)
      expect(colors).toEqual({
        fill: '#1A365D',
        stroke: '#90CDF4',
        text: '#BEE3F8',
      })
    })

    it('should return gray badge colors', () => {
      const colors = getBadgeColors('gray', mockTheme, colorMode)
      expect(colors).toEqual({
        fill: '#171923',
        stroke: '#E2E8F0',
        text: '#EDF2F7',
      })
    })

    it('should return green badge colors', () => {
      const colors = getBadgeColors('green', mockTheme, colorMode)
      expect(colors).toEqual({
        fill: '#1C4532',
        stroke: '#9AE6B4',
        text: '#C6F6D5',
      })
    })

    it('should return red badge colors', () => {
      const colors = getBadgeColors('red', mockTheme, colorMode)
      expect(colors).toEqual({
        fill: '#742A2A',
        stroke: '#FC8181',
        text: '#FED7D7',
      })
    })

    it('should return default gray badge colors for unknown color scheme', () => {
      const colors = getBadgeColors('unknown', mockTheme, colorMode)
      expect(colors).toEqual({
        fill: '#171923',
        stroke: '#E2E8F0',
        text: '#EDF2F7',
      })
    })
  })
})

describe('getStateClass', () => {
  it('should return "initial" for INITIAL state type', () => {
    const result = getStateClass('INITIAL' as FsmState.Type)
    expect(result).toBe('initial')
  })

  it('should return "success" for SUCCESS state type', () => {
    const result = getStateClass('SUCCESS' as FsmState.Type)
    expect(result).toBe('success')
  })

  it('should return "failed" for FAILED state type', () => {
    const result = getStateClass('FAILED' as FsmState.Type)
    expect(result).toBe('failed')
  })

  it('should return "intermediate" for INTERMEDIATE state type', () => {
    const result = getStateClass('INTERMEDIATE' as FsmState.Type)
    expect(result).toBe('intermediate')
  })

  it('should return "intermediate" for default case', () => {
    const result = getStateClass('UNKNOWN' as FsmState.Type)
    expect(result).toBe('intermediate')
  })
})

describe('getTransitionLabel', () => {
  it('should return event name when no guards', () => {
    const transition: FsmTransition = {
      fromState: 'StateA',
      toState: 'StateB',
      description: 'Test transition',
      event: 'TestEvent',
    }
    const result = getTransitionLabel(transition)
    expect(result).toBe('TestEvent')
  })

  it('should include guards in label when present', () => {
    const transition: FsmTransition & { guards?: string } = {
      fromState: 'StateA',
      toState: 'StateB',
      description: 'Test transition',
      event: 'TestEvent',
      guards: 'guardCondition',
    }
    const result = getTransitionLabel(transition)
    expect(result).toBe('TestEvent<br/>+ guardCondition')
  })

  it('should handle empty guards string', () => {
    const transition: FsmTransition & { guards?: string } = {
      fromState: 'StateA',
      toState: 'StateB',
      description: 'Test transition',
      event: 'TestEvent',
      guards: '',
    }
    const result = getTransitionLabel(transition)
    expect(result).toBe('TestEvent')
  })

  it('should handle complex event names', () => {
    const transition: FsmTransition = {
      fromState: 'StateA',
      toState: 'StateB',
      description: 'Test transition',
      event: 'Complex.Event.Name',
    }
    const result = getTransitionLabel(transition)
    expect(result).toBe('Complex.Event.Name')
  })

  it('should handle complex guards with multiple conditions', () => {
    const transition: FsmTransition & { guards?: string } = {
      fromState: 'StateA',
      toState: 'StateB',
      description: 'Test transition',
      event: 'TestEvent',
      guards: 'condition1 && condition2',
    }
    const result = getTransitionLabel(transition)
    expect(result).toBe('TestEvent<br/>+ condition1 && condition2')
  })
})
