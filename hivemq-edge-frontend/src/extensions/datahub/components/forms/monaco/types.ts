import type { Monaco } from '@monaco-editor/react'
import type { editor } from 'monaco-editor'

export type MonacoInstance = Monaco
export type EditorInstance = editor.IStandaloneCodeEditor

export interface MonacoConfig {
  configureLanguages: (monaco: MonacoInstance) => void
  configureThemes: (monaco: MonacoInstance, options?: ThemeOptions) => void
  getEditorOptions: (language: string, isReadOnly: boolean) => editor.IStandaloneEditorConstructionOptions
}

export interface ThemeOptions {
  backgroundColor?: string
  isReadOnly?: boolean
}

export interface LanguageConfig {
  configure: (monaco: MonacoInstance) => void
}
