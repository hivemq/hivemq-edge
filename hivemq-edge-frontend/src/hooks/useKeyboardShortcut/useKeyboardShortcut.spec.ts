import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useKeyboardShortcut, useKeyboardShortcuts, type KeyboardShortcut } from './useKeyboardShortcut'

describe('useKeyboardShortcut', () => {
  let originalPlatform: string

  beforeEach(() => {
    originalPlatform = navigator.platform
  })

  afterEach(() => {
    Object.defineProperty(navigator, 'platform', {
      value: originalPlatform,
      writable: true,
      configurable: true,
    })
    vi.clearAllMocks()
  })

  const mockPlatform = (platform: string) => {
    Object.defineProperty(navigator, 'platform', {
      value: platform,
      writable: true,
      configurable: true,
    })
  }

  const createKeyboardEvent = (
    key: string,
    modifiers: {
      ctrlKey?: boolean
      metaKey?: boolean
      shiftKey?: boolean
      altKey?: boolean
    } = {}
  ): KeyboardEvent => {
    return new KeyboardEvent('keydown', {
      key,
      ctrlKey: modifiers.ctrlKey || false,
      metaKey: modifiers.metaKey || false,
      shiftKey: modifiers.shiftKey || false,
      altKey: modifiers.altKey || false,
      bubbles: true,
      cancelable: true,
    })
  }

  describe('single shortcut', () => {
    it('should call callback when key is pressed without modifiers', () => {
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 'a',
        callback,
        description: 'Test shortcut',
      }

      renderHook(() => useKeyboardShortcut(shortcut))

      const event = createKeyboardEvent('a')
      window.dispatchEvent(event)

      expect(callback).toHaveBeenCalledTimes(1)
    })

    it('should call callback with Ctrl modifier on Windows', () => {
      mockPlatform('Win32')
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 'l',
        ctrl: true,
        callback,
        description: 'Apply layout',
      }

      renderHook(() => useKeyboardShortcut(shortcut))

      const event = createKeyboardEvent('l', { ctrlKey: true })
      window.dispatchEvent(event)

      expect(callback).toHaveBeenCalledTimes(1)
    })

    it('should call callback with Cmd (Meta) modifier on Mac', () => {
      mockPlatform('MacIntel')
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 'l',
        ctrl: true,
        callback,
        description: 'Apply layout',
      }

      renderHook(() => useKeyboardShortcut(shortcut))

      const event = createKeyboardEvent('l', { metaKey: true })
      window.dispatchEvent(event)

      expect(callback).toHaveBeenCalledTimes(1)
    })

    it('should not call callback when Ctrl is required but not pressed on Windows', () => {
      mockPlatform('Win32')
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 'l',
        ctrl: true,
        callback,
        description: 'Apply layout',
      }

      renderHook(() => useKeyboardShortcut(shortcut))

      const event = createKeyboardEvent('l')
      window.dispatchEvent(event)

      expect(callback).not.toHaveBeenCalled()
    })

    it('should not call callback when Cmd is required but not pressed on Mac', () => {
      mockPlatform('MacIntel')
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 'l',
        ctrl: true,
        callback,
        description: 'Apply layout',
      }

      renderHook(() => useKeyboardShortcut(shortcut))

      const event = createKeyboardEvent('l')
      window.dispatchEvent(event)

      expect(callback).not.toHaveBeenCalled()
    })

    it('should call callback with Shift modifier', () => {
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 's',
        shift: true,
        callback,
        description: 'Save with shift',
      }

      renderHook(() => useKeyboardShortcut(shortcut))

      const event = createKeyboardEvent('s', { shiftKey: true })
      window.dispatchEvent(event)

      expect(callback).toHaveBeenCalledTimes(1)
    })

    it('should not call callback when Shift is required but not pressed', () => {
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 's',
        shift: true,
        callback,
        description: 'Save with shift',
      }

      renderHook(() => useKeyboardShortcut(shortcut))

      const event = createKeyboardEvent('s')
      window.dispatchEvent(event)

      expect(callback).not.toHaveBeenCalled()
    })

    it('should call callback with Alt modifier', () => {
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 'f',
        alt: true,
        callback,
        description: 'Alt+F',
      }

      renderHook(() => useKeyboardShortcut(shortcut))

      const event = createKeyboardEvent('f', { altKey: true })
      window.dispatchEvent(event)

      expect(callback).toHaveBeenCalledTimes(1)
    })

    it('should call callback with multiple modifiers', () => {
      mockPlatform('Win32')
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 's',
        ctrl: true,
        shift: true,
        callback,
        description: 'Ctrl+Shift+S',
      }

      renderHook(() => useKeyboardShortcut(shortcut))

      const event = createKeyboardEvent('s', { ctrlKey: true, shiftKey: true })
      window.dispatchEvent(event)

      expect(callback).toHaveBeenCalledTimes(1)
    })

    it('should not call callback when not all required modifiers are pressed', () => {
      mockPlatform('Win32')
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 's',
        ctrl: true,
        shift: true,
        callback,
        description: 'Ctrl+Shift+S',
      }

      renderHook(() => useKeyboardShortcut(shortcut))

      // Only Ctrl pressed, Shift missing
      const event = createKeyboardEvent('s', { ctrlKey: true })
      window.dispatchEvent(event)

      expect(callback).not.toHaveBeenCalled()
    })

    it('should be case-insensitive for key matching', () => {
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 'L',
        callback,
        description: 'Test uppercase',
      }

      renderHook(() => useKeyboardShortcut(shortcut))

      const event = createKeyboardEvent('l')
      window.dispatchEvent(event)

      expect(callback).toHaveBeenCalledTimes(1)
    })

    it('should prevent default behavior when shortcut matches', () => {
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 's',
        ctrl: true,
        callback,
        description: 'Save',
      }

      mockPlatform('Win32')
      renderHook(() => useKeyboardShortcut(shortcut))

      const event = createKeyboardEvent('s', { ctrlKey: true })
      const preventDefaultSpy = vi.spyOn(event, 'preventDefault')
      const stopPropagationSpy = vi.spyOn(event, 'stopPropagation')

      window.dispatchEvent(event)

      expect(preventDefaultSpy).toHaveBeenCalled()
      expect(stopPropagationSpy).toHaveBeenCalled()
      expect(callback).toHaveBeenCalled()
    })

    it('should clean up event listener on unmount', () => {
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 'a',
        callback,
        description: 'Test',
      }

      const { unmount } = renderHook(() => useKeyboardShortcut(shortcut))

      unmount()

      const event = createKeyboardEvent('a')
      window.dispatchEvent(event)

      expect(callback).not.toHaveBeenCalled()
    })
  })

  describe('multiple shortcuts', () => {
    it('should register multiple shortcuts and call correct callback', () => {
      const callback1 = vi.fn()
      const callback2 = vi.fn()
      const callback3 = vi.fn()

      const shortcuts: KeyboardShortcut[] = [
        { key: 'a', callback: callback1, description: 'Shortcut A' },
        { key: 'b', callback: callback2, description: 'Shortcut B' },
        { key: 'c', ctrl: true, callback: callback3, description: 'Ctrl+C' },
      ]

      mockPlatform('Win32')
      renderHook(() => useKeyboardShortcuts(shortcuts))

      // Trigger 'a'
      window.dispatchEvent(createKeyboardEvent('a'))
      expect(callback1).toHaveBeenCalledTimes(1)
      expect(callback2).not.toHaveBeenCalled()
      expect(callback3).not.toHaveBeenCalled()

      // Trigger 'b'
      window.dispatchEvent(createKeyboardEvent('b'))
      expect(callback1).toHaveBeenCalledTimes(1)
      expect(callback2).toHaveBeenCalledTimes(1)
      expect(callback3).not.toHaveBeenCalled()

      // Trigger Ctrl+C
      window.dispatchEvent(createKeyboardEvent('c', { ctrlKey: true }))
      expect(callback1).toHaveBeenCalledTimes(1)
      expect(callback2).toHaveBeenCalledTimes(1)
      expect(callback3).toHaveBeenCalledTimes(1)
    })

    it('should only trigger first matching shortcut', () => {
      const callback1 = vi.fn()
      const callback2 = vi.fn()

      // Both shortcuts use same key
      const shortcuts: KeyboardShortcut[] = [
        { key: 'a', callback: callback1, description: 'First A' },
        { key: 'a', callback: callback2, description: 'Second A' },
      ]

      renderHook(() => useKeyboardShortcuts(shortcuts))

      window.dispatchEvent(createKeyboardEvent('a'))

      expect(callback1).toHaveBeenCalledTimes(1)
      expect(callback2).not.toHaveBeenCalled()
    })

    it('should handle platform-specific Ctrl/Cmd for multiple shortcuts on Mac', () => {
      mockPlatform('MacIntel')
      const callback1 = vi.fn()
      const callback2 = vi.fn()

      const shortcuts: KeyboardShortcut[] = [
        { key: 's', ctrl: true, callback: callback1, description: 'Save' },
        { key: 'o', ctrl: true, callback: callback2, description: 'Open' },
      ]

      renderHook(() => useKeyboardShortcuts(shortcuts))

      // Cmd+S on Mac
      window.dispatchEvent(createKeyboardEvent('s', { metaKey: true }))
      expect(callback1).toHaveBeenCalledTimes(1)
      expect(callback2).not.toHaveBeenCalled()

      // Cmd+O on Mac
      window.dispatchEvent(createKeyboardEvent('o', { metaKey: true }))
      expect(callback1).toHaveBeenCalledTimes(1)
      expect(callback2).toHaveBeenCalledTimes(1)
    })

    it('should prevent default and stop propagation for matching shortcuts', () => {
      const callback = vi.fn()
      const shortcuts: KeyboardShortcut[] = [{ key: 'l', ctrl: true, callback, description: 'Layout' }]

      mockPlatform('Win32')
      renderHook(() => useKeyboardShortcuts(shortcuts))

      const event = createKeyboardEvent('l', { ctrlKey: true })
      const preventDefaultSpy = vi.spyOn(event, 'preventDefault')
      const stopPropagationSpy = vi.spyOn(event, 'stopPropagation')

      window.dispatchEvent(event)

      expect(preventDefaultSpy).toHaveBeenCalled()
      expect(stopPropagationSpy).toHaveBeenCalled()
      expect(callback).toHaveBeenCalled()
    })

    it('should clean up all event listeners on unmount', () => {
      const callback1 = vi.fn()
      const callback2 = vi.fn()

      const shortcuts: KeyboardShortcut[] = [
        { key: 'a', callback: callback1, description: 'A' },
        { key: 'b', callback: callback2, description: 'B' },
      ]

      const { unmount } = renderHook(() => useKeyboardShortcuts(shortcuts))

      unmount()

      window.dispatchEvent(createKeyboardEvent('a'))
      window.dispatchEvent(createKeyboardEvent('b'))

      expect(callback1).not.toHaveBeenCalled()
      expect(callback2).not.toHaveBeenCalled()
    })
  })

  describe('edge cases', () => {
    it('should handle special keys like Enter', () => {
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 'Enter',
        callback,
        description: 'Submit',
      }

      renderHook(() => useKeyboardShortcut(shortcut))

      window.dispatchEvent(createKeyboardEvent('Enter'))

      expect(callback).toHaveBeenCalledTimes(1)
    })

    it('should handle special keys like Escape', () => {
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 'Escape',
        callback,
        description: 'Cancel',
      }

      renderHook(() => useKeyboardShortcut(shortcut))

      window.dispatchEvent(createKeyboardEvent('Escape'))

      expect(callback).toHaveBeenCalledTimes(1)
    })

    it('should not trigger when wrong key is pressed', () => {
      const callback = vi.fn()
      const shortcut: KeyboardShortcut = {
        key: 'a',
        callback,
        description: 'Test',
      }

      renderHook(() => useKeyboardShortcut(shortcut))

      window.dispatchEvent(createKeyboardEvent('b'))

      expect(callback).not.toHaveBeenCalled()
    })

    it('should update when shortcut changes', () => {
      const callback1 = vi.fn()
      const callback2 = vi.fn()

      const shortcut1: KeyboardShortcut = {
        key: 'a',
        callback: callback1,
        description: 'First',
      }

      const { rerender } = renderHook((props: { shortcut: KeyboardShortcut }) => useKeyboardShortcut(props.shortcut), {
        initialProps: { shortcut: shortcut1 },
      })

      window.dispatchEvent(createKeyboardEvent('a'))
      expect(callback1).toHaveBeenCalledTimes(1)

      // Update to new shortcut
      const shortcut2: KeyboardShortcut = {
        key: 'b',
        callback: callback2,
        description: 'Second',
      }

      rerender({ shortcut: shortcut2 })

      // Old shortcut should not work
      window.dispatchEvent(createKeyboardEvent('a'))
      expect(callback1).toHaveBeenCalledTimes(1) // Still 1

      // New shortcut should work
      window.dispatchEvent(createKeyboardEvent('b'))
      expect(callback2).toHaveBeenCalledTimes(1)
    })

    it('should handle empty shortcuts array gracefully', () => {
      const shortcuts: KeyboardShortcut[] = []

      expect(() => {
        renderHook(() => useKeyboardShortcuts(shortcuts))
        window.dispatchEvent(createKeyboardEvent('a'))
      }).not.toThrow()
    })
  })
})
