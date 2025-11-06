import type { MonacoInstance } from '../types'
import { DATAHUB_TYPES } from '../types/datahub-types'
import { configureDataHubFeatures } from './datahub-commands'

/**
 * Configure JavaScript/TypeScript language features
 */
export const configureJavaScript = (monaco: MonacoInstance) => {
  // Set compiler options for better IntelliSense
  monaco.languages.typescript.javascriptDefaults.setCompilerOptions({
    target: monaco.languages.typescript.ScriptTarget.ES2020,
    allowNonTsExtensions: true,
    moduleResolution: monaco.languages.typescript.ModuleResolutionKind.NodeJs,
    module: monaco.languages.typescript.ModuleKind.CommonJS,
    noEmit: true,
    esModuleInterop: true,
    allowJs: true,
    checkJs: true, // Enable type checking for better IntelliSense
    strict: false, // Not too strict for user scripts
    noLib: false, // Include standard library
  })

  // Enable diagnostics for better error detection
  monaco.languages.typescript.javascriptDefaults.setDiagnosticsOptions({
    noSemanticValidation: false, // Enable semantic validation
    noSyntaxValidation: false, // Enable syntax validation
    noSuggestionDiagnostics: false, // Enable suggestion diagnostics
    diagnosticCodesToIgnore: [
      1108, // Return statement outside function (for snippets)
      1375, // 'await' expressions are only allowed within async functions
      2304, // Cannot find name (for user-defined variables)
      7006, // Parameter implicitly has 'any' type
    ],
  })

  // Add common browser/Node globals for better IntelliSense
  monaco.languages.typescript.javascriptDefaults.addExtraLib(
    `
      // Console API
      declare var console: {
        log(...data: any[]): void;
        error(...data: any[]): void;
        warn(...data: any[]): void;
        info(...data: any[]): void;
        debug(...data: any[]): void;
        trace(...data: any[]): void;
      };

      // Common browser globals
      declare var window: any;
      declare var document: any;

      // setTimeout/setInterval
      declare function setTimeout(handler: Function, timeout?: number, ...args: any[]): number;
      declare function setInterval(handler: Function, timeout?: number, ...args: any[]): number;
      declare function clearTimeout(handle: number): void;
      declare function clearInterval(handle: number): void;

      // JSON
      declare var JSON: {
        parse(text: string): any;
        stringify(value: any, replacer?: any, space?: string | number): string;
      };

      // Common utilities
      declare var Math: any;
      declare var Date: any;
      declare var RegExp: any;
      declare var Array: any;
      declare var Object: any;
      declare var String: any;
      declare var Number: any;
      declare var Boolean: any;
    `,
    'ts:filename/global.d.ts'
  )

  // Add DataHub Transform API type definitions for IntelliSense
  console.log('[DataHub JavaScript Config] Loading type definitions...')
  console.log('[DataHub JavaScript Config] Type definitions length:', DATAHUB_TYPES.length)

  monaco.languages.typescript.javascriptDefaults.addExtraLib(DATAHUB_TYPES, 'ts:filename/datahub-transforms.d.ts')

  console.log('[DataHub JavaScript Config] Type definitions loaded successfully')

  // Register DataHub-specific commands and code actions
  configureDataHubFeatures(monaco)

  console.log('[DataHub JavaScript Config] Configuration complete!')
}
