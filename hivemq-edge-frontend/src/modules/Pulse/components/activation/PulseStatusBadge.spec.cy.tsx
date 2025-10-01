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
        activation: PulseStatus.activation.ERROR,
        runtime: PulseStatus.runtime.ERROR,
      },
      expected: 'Error',
    },
    {
      status: {
        activation: PulseStatus.activation.ERROR,
        runtime: PulseStatus.runtime.CONNECTED,
      },
      expected: 'Error',
    },
    {
      status: {
        activation: PulseStatus.activation.ERROR,
        runtime: PulseStatus.runtime.DISCONNECTED,
      },
      expected: 'Error',
    },
    {
      status: {
        activation: PulseStatus.activation.ERROR,
        runtime: PulseStatus.runtime.ERROR,
      },
      expected: 'Error',
    },

    {
      status: {
        activation: PulseStatus.activation.ACTIVATED,
        runtime: PulseStatus.runtime.ERROR,
      },
      expected: 'Error',
    },
    {
      status: {
        activation: PulseStatus.activation.ACTIVATED,
        runtime: PulseStatus.runtime.CONNECTED,
      },
      expected: 'Connected',
    },
    {
      status: {
        activation: PulseStatus.activation.ACTIVATED,
        runtime: PulseStatus.runtime.DISCONNECTED,
      },
      expected: 'Disconnected',
    },
    {
      status: {
        activation: PulseStatus.activation.ACTIVATED,
        runtime: PulseStatus.runtime.ERROR,
      },
      expected: 'Error',
    },

    {
      status: {
        activation: PulseStatus.activation.DEACTIVATED,
        runtime: PulseStatus.runtime.ERROR,
      },
      expected: 'Deactivated',
    },
    {
      status: {
        activation: PulseStatus.activation.DEACTIVATED,
        runtime: PulseStatus.runtime.CONNECTED,
      },
      expected: 'Deactivated',
    },
    {
      status: {
        activation: PulseStatus.activation.DEACTIVATED,
        runtime: PulseStatus.runtime.DISCONNECTED,
      },
      expected: 'Deactivated',
    },
    {
      status: {
        activation: PulseStatus.activation.DEACTIVATED,
        runtime: PulseStatus.runtime.ERROR,
      },
      expected: 'Deactivated',
    },
  ]

  it.each(testCases)(
    ({ status }) => `should render  ${status.activation} / ${status.runtime}`,
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
          .should('have.attr', 'data-activation', status.activation)
          .should('have.attr', 'data-runtime', status.runtime)
        cy.wrap(w[1])
          .should('have.attr', 'data-activation', status.activation)
          .should('have.attr', 'data-runtime', status.runtime)
      })
    }
  )
})
