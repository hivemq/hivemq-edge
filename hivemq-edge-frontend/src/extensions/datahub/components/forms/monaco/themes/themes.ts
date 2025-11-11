import type { MonacoInstance, ThemeOptions } from '../types'

/**
 * Define custom themes for Monaco Editor
 */
export const configureThemes = (monaco: MonacoInstance, options?: ThemeOptions) => {
  const { backgroundColor = '#ffffff', isReadOnly = false } = options || {}

  // Light theme for normal editing
  monaco.editor.defineTheme('lightTheme', {
    base: 'vs',
    inherit: true,
    rules: [],
    colors: {
      'editor.background': backgroundColor,
    },
  })

  // Read-only theme with muted colors
  if (isReadOnly) {
    monaco.editor.defineTheme('readOnlyTheme', {
      base: 'vs',
      inherit: false,
      rules: [
        { token: '', foreground: '#808080' }, // Gray for all tokens
      ],
      colors: {
        'editor.foreground': '#808080',
        'editor.background': backgroundColor,
      },
    })
  }
}

/**
 * Get theme name based on editor state
 */
export const getThemeName = (isReadOnly: boolean): string => {
  return isReadOnly ? 'readOnlyTheme' : 'lightTheme'
}
