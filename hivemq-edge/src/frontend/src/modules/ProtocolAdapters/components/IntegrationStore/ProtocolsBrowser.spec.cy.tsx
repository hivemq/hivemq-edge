/// <reference types="cypress" />

import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import ProtocolsBrowser from '@/modules/ProtocolAdapters/components/IntegrationStore/ProtocolsBrowser.tsx'
import { ProtocolAdapter } from '@/api/__generated__'

const MOCK_ADAPTERS: ProtocolAdapter[] = [
  {
    ...mockProtocolAdapter,
    id: 'simulation1',
    name: 'Simulation Server 1',
    category: { name: 'cat1', displayName: 'cat1' },
    tags: ['tag1'],
  },
  {
    ...mockProtocolAdapter,
    id: 'simulation2',
    name: 'Simulation Server 2',
    category: { name: 'cat2', displayName: 'cat2' },
    tags: ['tag2'],
  },
  {
    ...mockProtocolAdapter,
    id: 'simulation3',
    name: 'Fake Simulation Server 3',
    category: { name: 'cat1', displayName: 'cat1' },
    tags: ['tag3'],
  },
  {
    ...mockProtocolAdapter,
    id: 'simulation4',
    name: 'Simulation Server 4',
    category: { name: 'cat3', displayName: 'cat3' },
    tags: ['tag2'],
  },
]

describe('ProtocolsBrowser', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    const mockOnCreate = cy.stub().as('createAdapter')

    cy.mountWithProviders(
      <ProtocolsBrowser items={MOCK_ADAPTERS} facet={{ search: 'from an edge device' }} onCreate={mockOnCreate} />
    )

    cy.getByTestId('protocol-create-adapter').should('have.length', 4)
  })

  it('should render trigger a create instance', () => {
    const mockOnCreate = cy.stub().as('createAdapter')

    cy.mountWithProviders(
      <ProtocolsBrowser items={MOCK_ADAPTERS} facet={{ search: 'from an edge device' }} onCreate={mockOnCreate} />
    )

    cy.getByTestId('protocol-create-adapter').eq(0).click()
    cy.get('@createAdapter').should('have.been.calledWith', 'simulation1')

    cy.getByTestId('protocol-create-adapter').eq(3).click()
    cy.get('@createAdapter').should('have.been.calledWith', 'simulation4')
  })

  it('should filter properly by category', () => {
    const mockOnCreate = cy.stub().as('createAdapter')

    cy.mountWithProviders(
      <ProtocolsBrowser
        items={MOCK_ADAPTERS}
        onCreate={mockOnCreate}
        facet={{ filter: { key: 'category', value: 'cat1' } }}
      />
    )

    cy.getByTestId('protocol-create-adapter').should('have.length', 2)
    cy.getByTestId('protocol-create-adapter').eq(1).click()
    cy.get('@createAdapter').should('have.been.calledWith', 'simulation3')
  })

  it('should filter properly by tags', () => {
    const mockOnCreate = cy.stub().as('createAdapter')

    cy.mountWithProviders(
      <ProtocolsBrowser
        items={MOCK_ADAPTERS}
        onCreate={mockOnCreate}
        facet={{ filter: { key: 'tags', value: 'tag2' } }}
      />
    )

    cy.getByTestId('protocol-create-adapter').should('have.length', 2)
    cy.getByTestId('protocol-create-adapter').eq(1).click()
    cy.get('@createAdapter').should('have.been.calledWith', 'simulation4')
  })

  it('should combine filter and search', () => {
    const mockOnCreate = cy.stub().as('createAdapter')

    cy.mountWithProviders(
      <ProtocolsBrowser
        items={MOCK_ADAPTERS}
        onCreate={mockOnCreate}
        facet={{ search: 'Fake', filter: { key: 'category', value: 'cat1' } }}
      />
    )

    cy.getByTestId('protocol-create-adapter').should('have.length', 1)
    cy.getByTestId('protocol-create-adapter').eq(0).click()
    cy.get('@createAdapter').should('have.been.calledWith', 'simulation3')
  })

  it('should render non-installed adapters', () => {
    const mockOnCreate = cy.stub()

    cy.mountWithProviders(
      <ProtocolsBrowser
        items={[
          ...MOCK_ADAPTERS.slice(0, 2),
          {
            ...mockProtocolAdapter,
            id: 'simulation-soon',
            name: 'Simulation Server Preview',
            version: 'Available Soon',
            installed: undefined,
          },
        ]}
        facet={{ search: undefined }}
        onCreate={mockOnCreate}
      />
    )
    cy.getByTestId('protocol-name').should('have.length', 3)
    cy.getByTestId('protocol-create-adapter').should('have.length', 2)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <ProtocolsBrowser items={MOCK_ADAPTERS} facet={{ search: 'from an edge device' }} onCreate={cy.stub()} />
    )

    cy.checkAccessibility()
    cy.percySnapshot('Component: ProtocolsBrowser')
  })
})
