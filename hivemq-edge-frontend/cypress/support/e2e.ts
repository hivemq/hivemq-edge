import 'cypress-axe'
import 'cypress-each'
import '@percy/cypress'
import 'cypress-real-events'
import '@cypress/code-coverage/support'
import '@4tw/cypress-drag-drop'

import './commands'

import installLogsCollector from 'cypress-terminal-report/src/installLogsCollector'
import { register as registerCypressGrep } from '@cypress/grep'

installLogsCollector({
  // Enable and cutomize to filter the verbose log when failing
  //  collectTypes: ['cy:log', 'cy:command'],
  // filterLog: ({ message }) => message.includes('a11y error!'),
})
registerCypressGrep()
