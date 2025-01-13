import { ReactFlowProvider } from 'reactflow'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import CopyPasteStatus from '@datahub/components/controls/CopyPasteStatus.tsx'
import { DesignerStatus } from '@datahub/types.ts'

const getWrapperWith = (status?: DesignerStatus) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    return (
      <MockStoreWrapper
        config={{
          initialState: {
            status: status,
          },
        }}
      >
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </MockStoreWrapper>
    )
  }
  return Wrapper
}

describe('CopyPasteStatus', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should render properly when no copied content', () => {
    cy.mountWithProviders(<CopyPasteStatus nbCopied={0} />, { wrapper: getWrapperWith(DesignerStatus.DRAFT) })
    cy.getByTestId('copy-paste-status').should('contain.text', '0')
  })

  it('should render properly with copied content', () => {
    cy.mountWithProviders(<CopyPasteStatus nbCopied={2} />, { wrapper: getWrapperWith(DesignerStatus.DRAFT) })
    cy.getByTestId('copy-paste-status').should('contain.text', '2')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<CopyPasteStatus nbCopied={2} />, { wrapper: getWrapperWith(DesignerStatus.DRAFT) })

    cy.checkAccessibility()
  })
})
