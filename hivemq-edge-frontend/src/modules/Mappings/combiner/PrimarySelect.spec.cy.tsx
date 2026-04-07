import type { ReactNode, JSXElementConstructor } from 'react'
import { FormControl, FormLabel } from '@chakra-ui/react'

import { type DataCombining, DataIdentifierReference } from '@/api/__generated__'
import { mockCombiner, mockCombinerMapping } from '@/api/hooks/useCombiners/__handlers__'
import type { CombinerContext } from '@/modules/Mappings/types'
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
      // Each option now shows: label + type badge ("Tag" / "Topic Filter")
      cy.get('[role="option"]').eq(0).should('contain.text', 'my/tag/t1').should('contain.text', 'Tag')
      cy.get('[role="option"]').eq(1).should('contain.text', 'my/tag/t3').should('contain.text', 'Tag')
      cy.get('[role="option"]').eq(2).should('contain.text', 'my/topic/+/temp').should('contain.text', 'Topic Filter')

      cy.get('[role="option"]').eq(1).click()
      cy.get('@onChange').should(
        'have.been.calledWith',
        {
          adapterId: undefined,
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
    // Selected value is now rendered as a PLCTag badge
    cy.get('label + div [data-testid="topic-wrapper"]')
      .should('be.visible')
      .should('contain.text', 'tag')
      .should('contain.text', 't3')
    cy.get('label + div input').should('have.attr', 'aria-label', 'The primary data key of the mapping')

    cy.get('label + div').click()
    cy.get('label + div [role="listbox"]').as('options').should('be.visible')

    cy.get('@onChange').should('not.have.been.called')

    cy.get('@options').within(() => {
      cy.get('[role="option"]').should('have.length', 3)
      cy.get('[role="option"]').eq(0).should('contain.text', 'my/tag/t1').should('contain.text', 'Tag')
      cy.get('[role="option"]').eq(1).should('contain.text', 'my/tag/t3').should('have.attr', 'aria-selected', 'true')
      cy.get('[role="option"]').eq(2).should('contain.text', 'my/topic/+/temp').should('contain.text', 'Topic Filter')

      cy.get('[role="option"]').eq(1).click()
      cy.get('@onChange').should(
        'have.been.calledWith',
        {
          adapterId: undefined,
          label: 'my/tag/t3',
          value: 'my/tag/t3',
          type: DataIdentifierReference.type.TAG,
        },
        { action: 'select-option', name: undefined, option: undefined }
      )
    })

    cy.get('label + div [role="listbox"]').should('not.exist')
  })

  it('should build options from formContext.selectedSources when available', () => {
    const onChange = cy.stub().as('onChange')
    const formContext: CombinerContext = {
      selectedSources: {
        tags: [
          { id: 'my/tag/t1', type: DataIdentifierReference.type.TAG, scope: 'my-adapter' },
          { id: 'my/tag/t3', type: DataIdentifierReference.type.TAG, scope: 'other-adapter' },
        ],
        topicFilters: [{ id: 'my/topic/+/temp', type: DataIdentifierReference.type.TOPIC_FILTER }],
      },
    }

    cy.mountWithProviders(
      <PrimarySelect formData={mockCombinerMapping} formContext={formContext} onChange={onChange} />,
      { wrapper }
    )

    // Selected primary renders as scoped PLCTag badge
    cy.get('label + div [data-testid="topic-wrapper"]')
      .should('be.visible')
      .should('contain.text', 'my-adapter')
      .should('contain.text', 't1')

    cy.get('label + div').click()
    cy.get('label + div [role="listbox"]').as('options').should('be.visible')

    cy.get('@options').within(() => {
      cy.get('[role="option"]').should('have.length', 3)
      // Options from selectedSources carry adapter scope and type badge
      cy.get('[role="option"]')
        .eq(0)
        .should('contain.text', 'my/tag/t1')
        .should('contain.text', 'my-adapter')
        .should('contain.text', 'Tag')
      cy.get('[role="option"]')
        .eq(1)
        .should('contain.text', 'my/tag/t3')
        .should('contain.text', 'other-adapter')
        .should('contain.text', 'Tag')
      cy.get('[role="option"]').eq(2).should('contain.text', 'my/topic/+/temp').should('contain.text', 'Topic Filter')

      // onChange carries the adapterId from selectedSources
      cy.get('[role="option"]').eq(1).click()
      cy.get('@onChange').should(
        'have.been.calledWith',
        {
          adapterId: 'other-adapter',
          label: 'my/tag/t3',
          value: 'my/tag/t3',
          type: DataIdentifierReference.type.TAG,
        },
        { action: 'select-option', name: undefined, option: undefined }
      )
    })
  })

  it.skip('should capture screenshot for documentation', () => {
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

    cy.mountWithProviders(<PrimarySelect formData={mockPrimary} onChange={cy.stub} />, { wrapper })

    // Open dropdown to show options
    cy.get('label + div').click()
    cy.get('label + div [role="listbox"]').should('be.visible')

    // Screenshot: Dropdown with tag and topic filter options
    cy.screenshot('combiner-primary-select', {
      overwrite: true,
      capture: 'viewport',
    })
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
