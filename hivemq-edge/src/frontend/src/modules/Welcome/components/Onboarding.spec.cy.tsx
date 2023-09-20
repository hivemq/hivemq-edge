/// <reference types="cypress" />
import { IoLinkOutline } from 'react-icons/io5'

import Onboarding from './Onboarding.tsx'
import { OnboardingFetchType } from '@/modules/Welcome/hooks/useOnboarding.tsx'

const MOCK_ONBOARDING: OnboardingFetchType = {
  data: [
    {
      header: 'Heading 1',
      sections: [
        {
          title: 'Task 1',
          label: 'Get Started',
          to: '/link1',
          leftIcon: <IoLinkOutline />,
        },
        {
          title: 'Task  2',
          label: 'Get Started',
          to: '/link2',
          leftIcon: <IoLinkOutline />,
        },
      ],
    },
    {
      header: 'Heading 2',
      sections: [
        {
          title: 'Task 3',
          label: 'Get Started',
          to: '/link3',
          leftIcon: <IoLinkOutline />,
        },
      ],
    },
  ],
}

describe('Onboarding', () => {
  beforeEach(() => {
    cy.viewport(500, 800)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<Onboarding tasks={MOCK_ONBOARDING} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: Onboarding')
  })

  it('should render properly', () => {
    cy.mountWithProviders(<Onboarding tasks={MOCK_ONBOARDING} />)
    cy.get('a[aria-label="Get Started"]').should('have.length', 3)
    cy.get('a[aria-label="Get Started"]').eq(0).should('have.attr', 'href', '/link1')
    cy.get('a[aria-label="Get Started"]').eq(1).should('have.attr', 'href', '/link2')
    cy.get('a[aria-label="Get Started"]').eq(2).should('have.attr', 'href', '/link3')
  })
})
