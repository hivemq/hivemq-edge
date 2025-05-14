import { getByTestId } from './commands/getByTestId.ts'
import { getByAriaLabel } from './commands/getByAriaLabel.ts'
import { checkAccessibility } from './commands/checkAccessibility.ts'
import { clearInterceptList } from './commands/clearInterceptList.ts'

Cypress.Commands.add('getByTestId', getByTestId)
Cypress.Commands.add('getByAriaLabel', getByAriaLabel)
Cypress.Commands.add('checkAccessibility', checkAccessibility)
Cypress.Commands.add('clearInterceptList', clearInterceptList)
