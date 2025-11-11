import { describe, it, expect, vi } from 'vitest'
import { configureThemes, getThemeName } from './themes'
import type { MonacoInstance } from '../types'

describe('Monaco Themes', () => {
  describe('configureThemes', () => {
    it('should define lightTheme with default background', () => {
      const mockMonaco = {
        editor: {
          defineTheme: vi.fn(),
        },
      } as unknown as MonacoInstance

      configureThemes(mockMonaco)

      expect(mockMonaco.editor.defineTheme).toHaveBeenCalledWith('lightTheme', {
        base: 'vs',
        inherit: true,
        rules: [],
        colors: {
          'editor.background': '#ffffff',
        },
      })
    })

    it('should define lightTheme with custom background', () => {
      const mockMonaco = {
        editor: {
          defineTheme: vi.fn(),
        },
      } as unknown as MonacoInstance

      configureThemes(mockMonaco, { backgroundColor: '#f0f0f0' })

      expect(mockMonaco.editor.defineTheme).toHaveBeenCalledWith('lightTheme', {
        base: 'vs',
        inherit: true,
        rules: [],
        colors: {
          'editor.background': '#f0f0f0',
        },
      })
    })

    it('should define readOnlyTheme when isReadOnly is true', () => {
      const mockMonaco = {
        editor: {
          defineTheme: vi.fn(),
        },
      } as unknown as MonacoInstance

      configureThemes(mockMonaco, { isReadOnly: true })

      expect(mockMonaco.editor.defineTheme).toHaveBeenCalledWith('readOnlyTheme', {
        base: 'vs',
        inherit: false,
        rules: [{ token: '', foreground: '#808080' }],
        colors: {
          'editor.foreground': '#808080',
          'editor.background': '#ffffff',
        },
      })
    })

    it('should not define readOnlyTheme when isReadOnly is false', () => {
      const mockMonaco = {
        editor: {
          defineTheme: vi.fn(),
        },
      } as unknown as MonacoInstance

      configureThemes(mockMonaco, { isReadOnly: false })

      expect(mockMonaco.editor.defineTheme).toHaveBeenCalledTimes(1)
      expect(mockMonaco.editor.defineTheme).toHaveBeenCalledWith('lightTheme', expect.any(Object))
    })

    it('should define readOnlyTheme with custom background when isReadOnly is true', () => {
      const mockMonaco = {
        editor: {
          defineTheme: vi.fn(),
        },
      } as unknown as MonacoInstance

      configureThemes(mockMonaco, { backgroundColor: '#e0e0e0', isReadOnly: true })

      expect(mockMonaco.editor.defineTheme).toHaveBeenCalledWith('readOnlyTheme', {
        base: 'vs',
        inherit: false,
        rules: [{ token: '', foreground: '#808080' }],
        colors: {
          'editor.foreground': '#808080',
          'editor.background': '#e0e0e0',
        },
      })
    })
  })

  describe('getThemeName', () => {
    it('should return lightTheme when isReadOnly is false', () => {
      expect(getThemeName(false)).toBe('lightTheme')
    })

    it('should return readOnlyTheme when isReadOnly is true', () => {
      expect(getThemeName(true)).toBe('readOnlyTheme')
    })
  })
})
