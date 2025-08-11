import { PulseStatus } from '@/api/__generated__'
import PulseStatusBadge from '@/modules/Pulse/components/activation/PulseStatusBadge.tsx'
import { HStack } from '@chakra-ui/react'

describe('PulseStatusBadge', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  interface TestStatus {
    status: PulseStatus
    expected: string
  }

  const testCases: TestStatus[] = [
    {
      status: {
        activation: { status: PulseStatus.activationStatus.ERROR },
        runtime: { status: PulseStatus.runtimeStatus.ERROR },
      },
      expected: 'Error',
    },
    {
      status: {
        activation: { status: PulseStatus.activationStatus.ERROR },
        runtime: { status: PulseStatus.runtimeStatus.CONNECTED },
      },
      expected: 'Error',
    },
    {
      status: {
        activation: { status: PulseStatus.activationStatus.ERROR },
        runtime: { status: PulseStatus.runtimeStatus.DISCONNECTED },
      },
      expected: 'Error',
    },
    {
      status: {
        activation: { status: PulseStatus.activationStatus.ERROR },
        runtime: { status: PulseStatus.runtimeStatus.ERROR },
      },
      expected: 'Error',
    },

    {
      status: {
        activation: { status: PulseStatus.activationStatus.ACTIVATED },
        runtime: { status: PulseStatus.runtimeStatus.ERROR },
      },
      expected: 'Error',
    },
    {
      status: {
        activation: { status: PulseStatus.activationStatus.ACTIVATED },
        runtime: { status: PulseStatus.runtimeStatus.CONNECTED },
      },
      expected: 'Connected',
    },
    {
      status: {
        activation: { status: PulseStatus.activationStatus.ACTIVATED },
        runtime: { status: PulseStatus.runtimeStatus.DISCONNECTED },
      },
      expected: 'Disconnected',
    },
    {
      status: {
        activation: { status: PulseStatus.activationStatus.ACTIVATED },
        runtime: { status: PulseStatus.runtimeStatus.ERROR },
      },
      expected: 'Error',
    },

    {
      status: {
        activation: { status: PulseStatus.activationStatus.DEACTIVATED },
        runtime: { status: PulseStatus.runtimeStatus.ERROR },
      },
      expected: 'Deactivated',
    },
    {
      status: {
        activation: { status: PulseStatus.activationStatus.DEACTIVATED },
        runtime: { status: PulseStatus.runtimeStatus.CONNECTED },
      },
      expected: 'Deactivated',
    },
    {
      status: {
        activation: { status: PulseStatus.activationStatus.DEACTIVATED },
        runtime: { status: PulseStatus.runtimeStatus.DISCONNECTED },
      },
      expected: 'Deactivated',
    },
    {
      status: {
        activation: { status: PulseStatus.activationStatus.DEACTIVATED },
        runtime: { status: PulseStatus.runtimeStatus.ERROR },
      },
      expected: 'Deactivated',
    },
  ]

  it.each(testCases)(
    ({ status }) => `should render  ${status.activation.status} / ${status.runtime.status}`,
    ({ status, expected }) => {
      cy.injectAxe()
      cy.mountWithProviders(
        <HStack>
          <PulseStatusBadge status={status} />
          <PulseStatusBadge status={status} skeleton />
        </HStack>
      )

      cy.getByTestId('pulse-status').then((w) => {
        cy.wrap(w[0])
          .should('have.text', expected)
          .should('have.attr', 'data-activation', status.activation.status)
          .should('have.attr', 'data-runtime', status.runtime.status)
        cy.wrap(w[1])
          .should('have.attr', 'data-activation', status.activation.status)
          .should('have.attr', 'data-runtime', status.runtime.status)
      })
    }
  )
})
