import { UploadStepper } from '@/components/rjsf/BatchSubscription/components/UploadStepper.tsx'
import { BatchModeStep, BatchModeSteps } from '@/components/rjsf/BatchSubscription/hooks/useBatchModeSteps.tsx'

const MOCK_STEPS: BatchModeSteps[] = [
  {
    id: BatchModeStep.UPLOAD,
    title: 'first step',
    description: 'about the first step',
    renderer: <div>The first step container</div>,
  },
  {
    id: BatchModeStep.VALIDATE,
    title: 'second step',
    description: 'about the second step',
    renderer: <div>The second step container</div>,
  },
  {
    id: BatchModeStep.CONFIRM,
    isFinal: true,
    title: 'final step',
    description: 'about the final step',
    renderer: <div>The final step container</div>,
  },
]

describe('UploadStepper', () => {
  beforeEach(() => {
    cy.viewport(600, 600)
  })

  it('should render the steps', () => {
    cy.mountWithProviders(<UploadStepper steps={MOCK_STEPS} activeStep={0} />)

    cy.getByAriaLabel('Progress').should('be.visible')
    cy.get('[aria-label="Progress"] > div[data-status]').as('steps').should('have.length', 2)
    cy.get('@steps').first().should('have.attr', 'data-status', 'active')
    cy.get('@steps').first().find('h3').should('contain.text', 'first step')
    cy.get('@steps').first().find('h3 + p').should('contain.text', 'about the first step')

    cy.get('@steps').eq(1).should('have.attr', 'data-status', 'incomplete')
    cy.get('@steps').eq(1).find('h3').should('contain.text', 'second step')
    cy.get('@steps').eq(1).find('h3 + p').should('contain.text', 'about the second step')

    cy.getByTestId('stepper-container').should('contain.text', 'The first step container')
  })

  it.only('should render the final step', () => {
    cy.mountWithProviders(<UploadStepper steps={MOCK_STEPS} activeStep={2} />)

    cy.getByAriaLabel('Progress').should('be.visible')
    cy.get('[aria-label="Progress"] > div[data-status]').as('steps').should('have.length', 2)
    cy.get('@steps').first().should('have.attr', 'data-status', 'complete')
    cy.get('@steps').eq(1).should('have.attr', 'data-status', 'complete')
    cy.getByTestId('stepper-container').should('contain.text', 'The final step container')
  })
})
