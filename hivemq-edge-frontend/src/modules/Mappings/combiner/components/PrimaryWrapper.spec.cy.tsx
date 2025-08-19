import { PrimaryWrapper } from '@/modules/Mappings/combiner/components/PrimaryWrapper.tsx'
import { Text } from '@chakra-ui/react'

describe('PrimaryWrapper', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    cy.mountWithProviders(
      <PrimaryWrapper isPrimary={true}>
        <Text data-testid="the-target">This is a content</Text>
      </PrimaryWrapper>
    )

    cy.getByTestId('primary-wrapper')
      .should('be.visible')
      .within(() => {
        cy.getByAriaLabel('Primary key').should('be.visible')
        cy.getByTestId('the-target').should('have.text', 'This is a content')
      })
  })

  it('should render properly if not a primary key', () => {
    cy.mountWithProviders(
      <PrimaryWrapper isPrimary={false}>
        <Text data-testid="the-target">This is a content</Text>
      </PrimaryWrapper>
    )

    cy.getByTestId('primary-wrapper').should('not.exist')
    cy.getByTestId('the-target').should('have.text', 'This is a content')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <PrimaryWrapper isPrimary={true}>
        <Text>This is a content</Text>
      </PrimaryWrapper>
    )

    cy.checkAccessibility(undefined, {
      rules: {
        'color-contrast': { enabled: false },
      },
    })
  })
})
