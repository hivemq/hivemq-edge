import { Modal, ModalCloseButton, ModalContent, ModalHeader, ModalOverlay } from '@chakra-ui/react'
import type { FC, PropsWithChildren } from 'react'
import SchemaMerger from './SchemaMerger'

// TODO[NVL] This is wrong: the modal should be a reusable component
const Wrapper: FC<PropsWithChildren> = ({ children }) => (
  <Modal isOpen={true} onClose={cy.stub} id="destination-schema">
    <ModalOverlay />
    <ModalContent>
      <ModalCloseButton />
      <ModalHeader>Merging</ModalHeader>
      {children}
    </ModalContent>
  </Modal>
)

describe('SchemaMerger', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render error properly', () => {
    const onUpload = cy.stub().as('onUpload')
    const onClose = cy.stub().as('onClose')
    cy.mountWithProviders(<SchemaMerger onUpload={onUpload} onClose={onClose} />, { wrapper: Wrapper })

    cy.get('[role="dialog"]#chakra-modal-destination-schema').should('be.visible')
    cy.get('[role="dialog"]#chakra-modal-destination-schema').within(() => {
      cy.get('header').should('have.text', 'Merging')
      cy.getByTestId('schema-infer-prompt').should(
        'have.text',
        'We can create a new schema based on the current state of the sources'
      )
      cy.getByTestId('schema-infer-merged').should('have.attr', 'data-invalid')
      cy.getByTestId('schema-infer-merged').within(() => {
        cy.get('label').should('contain.text', 'Suggested merged schema')
        cy.get('#schema-infer-properties-feedback').should('have.text', 'You need to define the source schemas first')
      })

      cy.get('footer').within(() => {
        cy.getByTestId('schema-infer-cancel').should('have.text', 'Cancel')
        cy.getByTestId('schema-infer-generate').should('have.text', 'Generate').should('be.disabled')

        cy.get('@onClose').should('not.have.been.called')
        cy.getByTestId('schema-infer-cancel').click()
        cy.get('@onClose').should('have.been.called')
      })
    })
  })

  it.skip('should render merged schema properly', () => {
    // TODO[NVL] add the tests
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<SchemaMerger onUpload={cy.stub} onClose={cy.stub} />, { wrapper: Wrapper })

    cy.get('label + div').click()
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] ReactSelect not tagging properly the listbox
        'aria-input-field-name': { enabled: false },
      },
    })
  })
})
