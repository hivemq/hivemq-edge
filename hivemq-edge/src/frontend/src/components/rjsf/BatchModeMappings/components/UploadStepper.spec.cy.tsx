import type { FC } from 'react'
import { UploadStepper } from '@/components/rjsf/BatchModeMappings/components/UploadStepper.tsx'
import type { BatchModeSteps, StepRendererProps, BatchModeStore } from '@/components/rjsf/BatchModeMappings/types.ts'
import { BatchModeStepType } from '@/components/rjsf/BatchModeMappings/types.ts'
import { MOCK_ID_SCHEMA } from '@/components/rjsf/BatchModeMappings/__test-utils__/store.mocks.ts'

const MOCK_STORE: BatchModeStore = {
  idSchema: MOCK_ID_SCHEMA,
  schema: {},
}

const First: FC<StepRendererProps> = () => <div>The first step container</div>
const Second: FC<StepRendererProps> = () => <div>The second step container</div>
const Final: FC<StepRendererProps> = () => <div>The final step container</div>

const MOCK_STEPS: BatchModeSteps[] = [
  {
    id: BatchModeStepType.UPLOAD,
    title: 'first step',
    description: 'about the first step',
    renderer: First,
  },
  {
    id: BatchModeStepType.VALIDATE,
    title: 'second step',
    description: 'about the second step',
    renderer: Second,
  },
  {
    id: BatchModeStepType.CONFIRM,
    isFinal: true,
    title: 'final step',
    description: 'about the final step',
    renderer: Final,
  },
]

describe('UploadStepper', () => {
  beforeEach(() => {
    cy.viewport(600, 600)
  })

  it('should render the steps', () => {
    cy.mountWithProviders(<UploadStepper steps={MOCK_STEPS} activeStep={0} onContinue={cy.stub()} store={MOCK_STORE} />)

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

  it('should render the final step', () => {
    cy.mountWithProviders(<UploadStepper steps={MOCK_STEPS} activeStep={2} onContinue={cy.stub()} store={MOCK_STORE} />)

    cy.getByAriaLabel('Progress').should('be.visible')
    cy.get('[aria-label="Progress"] > div[data-status]').as('steps').should('have.length', 2)
    cy.get('@steps').first().should('have.attr', 'data-status', 'complete')
    cy.get('@steps').eq(1).should('have.attr', 'data-status', 'complete')
    cy.getByTestId('stepper-container').should('contain.text', 'The final step container')
  })
})
