import { QuickFilters } from '@/modules/Workspace/components/filters/index.ts'
import type { FilterConfig } from '@/modules/Workspace/components/filters/types.ts'
import { KEY_FILTER_CURRENT } from '@/modules/Workspace/components/filters/types.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

const MOCK_FILTER: FilterConfig = {
  entities: {
    isActive: true,
    filter: [
      {
        label: 'Bridges',
        value: NodeTypes.BRIDGE_NODE,
      },
    ],
  },
  protocols: {
    isActive: true,
    filter: [
      {
        type: 'simulation',
        label: 'Simulation',
      },
    ],
  },
}
describe('QuickFilters', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    window.localStorage.setItem(KEY_FILTER_CURRENT, JSON.stringify(MOCK_FILTER))
  })

  it('should render properly', () => {
    cy.mountWithProviders(<QuickFilters isFilterActive={true} />)

    cy.getByTestId('workspace-quick-filters-container').within(() => {
      cy.get('h2').should('contain', 'Quick filters')
      cy.get('[role="list"] li').should('have.length', 0)

      cy.get('[role="group"] label#workspace-filter-configuration-label').should('have.text', 'Save filter')
    })
  })

  it('should render a first quick filter', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(<QuickFilters isFilterActive={true} onChange={onChange} />)

    cy.get('@onChange').should('not.have.been.called')
    cy.getByTestId('workspace-quick-filters-container').within(() => {
      cy.get('h2').should('contain', 'Quick filters')
      cy.get('[role="list"] li').should('have.length', 0)

      cy.get('[role="group"] label#workspace-filter-configuration-label').should('have.text', 'Save filter')
      cy.get('[role="group"] input#workspace-filter-configuration-input').type('test')
      cy.get('[role="group"] button#workspace-filter-configuration-save').click()
    })

    cy.get('@onChange').should('not.have.been.called')

    cy.getByTestId('workspace-quick-filters-container').within(() => {
      cy.get('[role="list"] li').should('have.length', 1)
      cy.get('[role="list"] li').eq(0).as('fistItem')
    })

    cy.get('@fistItem').within(() => {
      cy.getByTestId('workspace-filter-quick-label').should('have.text', 'test')
      cy.getByTestId('workspace-filter-quick-label').should('have.attr', 'data-checked')

      cy.getByTestId('workspace-filter-quick-label').click()
      cy.getByTestId('workspace-filter-quick-label').should('not.have.attr', 'data-checked')
      cy.get('@onChange').should('have.been.calledWithMatch', {
        label: 'test',
        isActive: false,
      })
    })
  })

  it('should remove a quick filter', () => {
    cy.mountWithProviders(<QuickFilters isFilterActive={true} />)

    cy.getByTestId('workspace-quick-filters-container').within(() => {
      cy.get('[role="group"] input#workspace-filter-configuration-input').type('test')
      cy.get('[role="group"] button#workspace-filter-configuration-save').click()
      cy.get('[role="list"] li').eq(0).as('fistItem')
    })

    cy.get('@fistItem').within(() => {
      cy.getByTestId('workspace-filter-quick-label').should('have.text', 'test')
      cy.getByTestId('workspace-filter-quick-label').should('have.attr', 'data-checked')

      cy.getByTestId('workspace-filter-quick-label').click()
      cy.getByTestId('workspace-filter-quick-label').should('not.have.attr', 'data-checked')
      cy.get('button#menu-button-filter-quick').click()

      cy.get('[role="menu"] button').should('have.length', 2)
      cy.get('[role="menu"] button').eq(1).click()
    })

    cy.get('[role="alertdialog"]').within(() => {
      cy.get('header').should('have.text', 'Delete the quick filter')
      cy.get('header + div').should(
        'have.text',
        'Do you really want to delete the quick filter? The action cannot be reverted.'
      )
      cy.get('footer button').should('have.length', 2)
      cy.get('footer button').eq(0).should('have.text', 'Cancel')
      cy.get('footer button').eq(0).click()
    })

    cy.get('[role="alertdialog"]').should('not.exist')
    cy.getByTestId('workspace-quick-filters-container').within(() => {
      cy.get('[role="list"] li').should('have.length', 1)
    })

    cy.get('@fistItem').within(() => {
      cy.get('button#menu-button-filter-quick').click()
      cy.get('[role="menu"] button').should('have.length', 2)
      cy.get('[role="menu"] button').eq(1).click()
    })

    cy.get('[role="alertdialog"]').within(() => {
      cy.get('header').should('have.text', 'Delete the quick filter')

      cy.get('footer button').eq(1).should('have.text', 'Delete')
      cy.get('footer button').eq(1).click()
    })

    cy.get('[role="alertdialog"]').should('not.exist')
    cy.getByTestId('workspace-quick-filters-container').within(() => {
      cy.get('[role="list"] li').should('have.length', 0)
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<QuickFilters isFilterActive={true} />)

    cy.checkAccessibility()
  })
})
