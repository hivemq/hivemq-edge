import type { ReactNode, JSXElementConstructor } from 'react'
import { FormControl, FormLabel } from '@chakra-ui/react'

import { type DataCombining, DataIdentifierReference } from '@/api/__generated__'
import { mockCombiner, mockCombinerMapping } from '@/api/hooks/useCombiners/__handlers__'
import { PrimarySelect } from './PrimarySelect'

// TODO[30982] Should not be needed; integrate label inside the component
const wrapper: JSXElementConstructor<{ children: ReactNode }> = ({ children }) => {
  return (
    <FormControl>
      <FormLabel>The Primary selector</FormLabel>
      {children}
    </FormControl>
  )
}

describe('PrimarySelect', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render an empty primary', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(<PrimarySelect formData={mockCombiner.mappings.items[0]} onChange={onChange} />, { wrapper })

    cy.get('label + div [role="listbox"]').should('not.exist')

    cy.get('label + div').click()
    cy.get('label + div input').should('have.attr', 'aria-label', 'The primary data key of the mapping')
    cy.get('label + div [role="listbox"]').as('options').should('be.visible')

    cy.get('@onChange').should('not.have.been.called')

    cy.get('@options').within(() => {
      cy.get('[role="option"]').should('have.length', 3)
      cy.get('[role="option"]').eq(0).should('have.text', 'my/tag/t1')
      cy.get('[role="option"]').eq(1).should('have.text', 'my/tag/t3')
      cy.get('[role="option"]').eq(2).should('have.text', 'my/topic/+/temp')

      cy.get('[role="option"]').eq(1).click()
      cy.get('@onChange').should(
        'have.been.calledWith',
        {
          label: 'my/tag/t3',
          value: 'my/tag/t3',
          type: DataIdentifierReference.type.TAG,
        },
        { action: 'select-option', name: undefined, option: undefined }
      )
    })

    cy.get('label + div [role="listbox"]').should('not.exist')
  })

  it('should render properly', () => {
    const onChange = cy.stub().as('onChange')
    const mockPrimary: DataCombining = {
      ...mockCombinerMapping,
      sources: {
        ...mockCombinerMapping.sources,
        primary: {
          id: 'my/tag/t3',
          type: DataIdentifierReference.type.TAG,
        },
      },
    }

    cy.mountWithProviders(<PrimarySelect formData={mockPrimary} onChange={onChange} />, { wrapper })

    cy.get('label + div [role="listbox"]').should('not.exist')
    cy.get('label + div').should('have.text', 'my/tag/t3')
    cy.get('label + div input').should('have.attr', 'aria-label', 'The primary data key of the mapping')

    cy.get('label + div').click()
    cy.get('label + div [role="listbox"]').as('options').should('be.visible')

    cy.get('@onChange').should('not.have.been.called')

    cy.get('@options').within(() => {
      cy.get('[role="option"]').should('have.length', 3)
      cy.get('[role="option"]').eq(0).should('have.text', 'my/tag/t1')
      cy.get('[role="option"]').eq(1).should('have.text', 'my/tag/t3').should('have.attr', 'aria-selected', 'true')
      cy.get('[role="option"]').eq(2).should('have.text', 'my/topic/+/temp')

      cy.get('[role="option"]').eq(1).click()
      cy.get('@onChange').should(
        'have.been.calledWith',
        {
          label: 'my/tag/t3',
          value: 'my/tag/t3',
          type: DataIdentifierReference.type.TAG,
        },
        { action: 'select-option', name: undefined, option: undefined }
      )
    })

    cy.get('label + div [role="listbox"]').should('not.exist')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<PrimarySelect formData={mockCombiner.mappings.items[0]} onChange={cy.stub} />, { wrapper })

    cy.get('label + div').click()
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] ReactSelect not tagging properly the listbox
        'aria-input-field-name': { enabled: false },
      },
    })
  })
})
