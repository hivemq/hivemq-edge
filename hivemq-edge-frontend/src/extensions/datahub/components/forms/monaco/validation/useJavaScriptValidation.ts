import { useCallback } from 'react'
import { useMonaco } from '@monaco-editor/react'
import { validateJavaScript, type ValidationResult } from './javascriptValidator'

/**
 * Hook to validate JavaScript code using Monaco's TypeScript language service
 *
 * This hook provides a safe way to validate JavaScript without executing it,
 * using Monaco Editor's built-in validation capabilities.
 *
 * @returns Object with validate function and Monaco ready state
 *
 * @example
 * ```typescript
 * const { validate, isReady } = useJavaScriptValidation()
 *
 * const handleValidate = async (code: string) => {
 *   if (!isReady) {
 *     console.warn('Monaco not ready yet')
 *     return
 *   }
 *
 *   const result = await validate(code)
 *   if (!result.isValid) {
 *     console.error('Validation failed:', result.errors)
 *   }
 * }
 * ```
 */
export const useJavaScriptValidation = () => {
  const monaco = useMonaco()

  const validate = useCallback(
    async (code: string): Promise<ValidationResult> => {
      if (!monaco) {
        // Monaco not loaded yet - return valid (graceful degradation)
        // This allows forms to work even if Monaco hasn't loaded yet
        return { isValid: true, errors: [], warnings: [] }
      }

      return validateJavaScript(monaco, code)
    },
    [monaco]
  )

  return {
    /**
     * Validate JavaScript code and return validation results
     */
    validate,
    /**
     * Whether Monaco is loaded and ready for validation
     */
    isReady: !!monaco,
  }
}
