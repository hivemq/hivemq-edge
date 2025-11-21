/// <reference types="cypress" />
import type { ContextObject } from 'axe-core'
import type { Options } from 'cypress-axe'
import type * as Sinon from 'sinon'

import { getByTestId } from './commands/getByTestId'
import { findByTestId } from './commands/findByTestId'
import { getByAriaLabel } from './commands/getByAriaLabel'
import { checkAccessibility } from './commands/checkAccessibility'
import { clearInterceptList } from './commands/clearInterceptList'
import { setMonacoEditorValue, getMonacoEditorValue } from './commands/monacoEditor'
import { saveHTMLSnapshot, logDOMState } from './commands/saveHTMLSnapshot'

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      getByTestId(value: string): Chainable<JQuery<HTMLElement>>
      findByTestId(testId: string): Chainable<JQuery<HTMLElement>>

      getByAriaLabel(value: string): Chainable<JQuery<HTMLElement>>

      checkAccessibility(
        context?: string | Node | ContextObject | undefined,
        options?: Options | undefined,
        skipFailures?: boolean
      ): Chainable<JQuery<HTMLElement>>

      clearInterceptList(interceptAlias: string): Chainable<JQuery<HTMLElement>>

      setMonacoEditorValue(value: string): Chainable<JQuery<HTMLElement>>

      getMonacoEditorValue(): Chainable<string>

      // For AI debugging
      saveHTMLSnapshot(name: string): Chainable<void>
      logDOMState(label?: string): Chainable<void>
    }
  }
}

Cypress.Commands.add('getByTestId', getByTestId)
Cypress.Commands.add('findByTestId', { prevSubject: 'element' }, findByTestId)
Cypress.Commands.add('getByAriaLabel', getByAriaLabel)
Cypress.Commands.add('checkAccessibility', checkAccessibility)
Cypress.Commands.add('clearInterceptList', clearInterceptList)
Cypress.Commands.add('setMonacoEditorValue', { prevSubject: 'element' }, setMonacoEditorValue)
Cypress.Commands.add('getMonacoEditorValue', { prevSubject: 'element' }, getMonacoEditorValue)
Cypress.Commands.add('saveHTMLSnapshot', saveHTMLSnapshot)
Cypress.Commands.add('logDOMState', logDOMState)

// eslint-disable-next-line @typescript-eslint/no-namespace, @typescript-eslint/no-unused-vars
declare namespace Chai {
  interface Assertion {
    calledWithErrorMessage(expectedMessage: string): Assertion
  }
}

/**
 * Check if a stub has been called an Error that has a specific message.
 */
chai.Assertion.addMethod('calledWithErrorMessage', function (expectedMessage: string) {
  const stub = this._obj as Sinon.SinonSpy

  this.assert(
    typeof stub.getCall === 'function' &&
      stub.getCalls().some((call) => call.args.some((arg) => arg instanceof Error && arg.message === expectedMessage)),
    'expected stub to have been called with Error(message=#{exp}), but got #{act}',
    'expected stub not to have been called with Error(message=#{exp})',
    expectedMessage,
    stub.getCalls().map((call) => call.args)
  )
})
