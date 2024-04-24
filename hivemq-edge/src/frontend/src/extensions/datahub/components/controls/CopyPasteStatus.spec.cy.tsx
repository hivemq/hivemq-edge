/// <reference types="cypress" />

import CopyPasteStatus from '@datahub/components/controls/CopyPasteStatus.tsx'
import { ReactFlowProvider } from 'reactflow'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <ReactFlowProvider>{children}</ReactFlowProvider>
)

describe('CopyPasteStatus', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should renders properly when no copied content', () => {
    cy.mountWithProviders(<CopyPasteStatus nbCopied={0} />, { wrapper })
    cy.getByTestId('copy-paste-status').should('contain.text', '0')
  })

  it('should renders properly with copied content', () => {
    cy.mountWithProviders(<CopyPasteStatus nbCopied={2} />, { wrapper })
    cy.getByTestId('copy-paste-status').should('contain.text', '2')
  })
})
