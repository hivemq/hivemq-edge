/**
 * Custom hook for keyboard shortcuts in the workspace
 */

import { useEffect } from 'react'

export interface KeyboardShortcut {
  key: string
  ctrl?: boolean
  shift?: boolean
  alt?: boolean
  meta?: boolean
  callback: () => void
  description: string
}

/**
 * Hook to register keyboard shortcuts
 * Handles cross-platform differences (Cmd on Mac, Ctrl on Windows/Linux)
 */
export const useKeyboardShortcut = (shortcut: KeyboardShortcut) => {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0

      // Check if the key matches
      const keyMatch = event.key.toLowerCase() === shortcut.key.toLowerCase()

      // Check modifiers
      const ctrlMatch = shortcut.ctrl ? (isMac ? event.metaKey : event.ctrlKey) : !(isMac ? event.metaKey : event.ctrlKey)
      const shiftMatch = shortcut.shift ? event.shiftKey : !event.shiftKey
      const altMatch = shortcut.alt ? event.altKey : !event.altKey
      const metaMatch = shortcut.meta ? event.metaKey : true

      if (keyMatch && ctrlMatch && shiftMatch && altMatch && metaMatch) {
        event.preventDefault()
        event.stopPropagation()
        shortcut.callback()
      }
    }

    window.addEventListener('keydown', handleKeyDown)

    return () => {
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [shortcut])
}

/**
 * Hook to register multiple keyboard shortcuts
 */
export const useKeyboardShortcuts = (shortcuts: KeyboardShortcut[]) => {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0

      for (const shortcut of shortcuts) {
        const keyMatch = event.key.toLowerCase() === shortcut.key.toLowerCase()
        const ctrlMatch = shortcut.ctrl ? (isMac ? event.metaKey : event.ctrlKey) : !event.ctrlKey
        const shiftMatch = shortcut.shift ? event.shiftKey : !event.shiftKey
        const altMatch = shortcut.alt ? event.altKey : !event.altKey

        if (keyMatch && ctrlMatch && shiftMatch && altMatch) {
          event.preventDefault()
          event.stopPropagation()
          shortcut.callback()
          break
        }
      }
    }

    window.addEventListener('keydown', handleKeyDown)

    return () => {
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [shortcuts])
}
