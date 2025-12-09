import type { Monaco } from '@monaco-editor/react'
import type monaco from 'monaco-editor'

export interface ValidationResult {
  isValid: boolean
  errors: ValidationError[]
  warnings: ValidationError[]
}

export interface ValidationError {
  message: string
  line: number
  column: number
  severity: 'error' | 'warning' | 'info'
  code?: string
}

/**
 * Validate JavaScript code using Monaco's TypeScript language service
 * without executing the code or requiring an active editor instance.
 *
 * This is a secure alternative to using `new Function()` or `eval()`,
 * as it only performs static analysis without executing any code.
 *
 * @param monacoInstance - The Monaco instance (from useMonaco hook)
 * @param code - The JavaScript code to validate
 * @param uri - Optional URI for the temporary model (default: inmemory://model/validation.js)
 * @returns Promise with validation results
 *
 * @example
 * ```typescript
 * const result = await validateJavaScript(monaco, 'function test() { return true; }')
 * if (!result.isValid) {
 *   console.error('Validation errors:', result.errors)
 * }
 * ```
 */
export const validateJavaScript = async (
  monacoInstance: Monaco,
  code: string,
  uri: string = 'inmemory://model/validation.js'
): Promise<ValidationResult> => {
  let model: monaco.editor.ITextModel | null = null

  try {
    const modelUri = monacoInstance.Uri.parse(uri)

    // Check if model already exists and dispose it
    const existingModel = monacoInstance.editor.getModel(modelUri)
    if (existingModel) {
      existingModel.dispose()
    }

    // Create temporary model for validation
    model = monacoInstance.editor.createModel(code, 'javascript', modelUri)

    // Wait for TypeScript language service to process the model
    // The language service runs in a Web Worker, so we need to wait for it to analyze the code
    // We use a Promise-based approach to wait for markers to be available
    await waitForValidation(monacoInstance, model)

    // Get diagnostics from Monaco
    const markers = monacoInstance.editor.getModelMarkers({
      resource: model.uri,
    })

    // Separate errors and warnings
    const errors: ValidationError[] = []
    const warnings: ValidationError[] = []

    markers.forEach((marker) => {
      const error: ValidationError = {
        message: marker.message,
        line: marker.startLineNumber,
        column: marker.startColumn,
        severity:
          marker.severity === monacoInstance.MarkerSeverity.Error
            ? 'error'
            : marker.severity === monacoInstance.MarkerSeverity.Warning
              ? 'warning'
              : 'info',
        code: marker.code?.toString(),
      }

      if (marker.severity === monacoInstance.MarkerSeverity.Error) {
        errors.push(error)
      } else if (marker.severity === monacoInstance.MarkerSeverity.Warning) {
        warnings.push(error)
      }
    })

    return {
      isValid: errors.length === 0,
      errors,
      warnings,
    }
  } finally {
    // Clean up: dispose the temporary model to prevent memory leaks
    if (model) {
      model.dispose()
    }
  }
}

/**
 * Wait for Monaco's TypeScript language service to complete validation
 * Uses polling with a timeout to detect when markers are available
 */
const waitForValidation = async (monacoInstance: Monaco, model: monaco.editor.ITextModel): Promise<void> => {
  const maxWaitTime = 2000 // 2 seconds max
  const checkInterval = 50 // Check every 50ms
  const startTime = Date.now()

  return new Promise((resolve) => {
    const checkMarkers = () => {
      const markers = monacoInstance.editor.getModelMarkers({
        resource: model.uri,
      })

      // If we have markers (errors/warnings) or if enough time has passed for validation
      // Note: Valid code will have 0 markers, so we wait a bit to be sure
      const elapsed = Date.now() - startTime
      const hasMarkers = markers.length > 0
      const hasWaitedMinimum = elapsed >= 100 // Minimum 100ms for language service
      const hasTimedOut = elapsed >= maxWaitTime

      if (hasMarkers || hasWaitedMinimum || hasTimedOut) {
        resolve()
      } else {
        setTimeout(checkMarkers, checkInterval)
      }
    }

    checkMarkers()
  })
}

/**
 * Format validation error for display in RJSF or other UI
 */
export const formatValidationError = (error: ValidationError): string => {
  return `Line ${error.line}, Column ${error.column}: ${error.message}`
}

/**
 * Check if code has any syntax errors (ignoring warnings)
 */
export const hasSyntaxErrors = (result: ValidationResult): boolean => {
  return result.errors.length > 0
}
