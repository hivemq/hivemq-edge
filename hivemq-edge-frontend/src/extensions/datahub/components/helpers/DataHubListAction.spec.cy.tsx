import { mockDataPolicy } from '@datahub/api/hooks/DataHubDataPoliciesService/__handlers__'
import { mockBehaviorPolicy } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/__handlers__'
import DataHubListAction from '@datahub/components/helpers/DataHubListAction.tsx'
import { type CombinedPolicy, PolicyType } from '@datahub/types.ts'

describe('DataHubListAction', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  context('DRAFT', () => {
    it('should render the actions for DRAFT', () => {
      const policy: CombinedPolicy = {
        type: PolicyType.CREATE_POLICY,
        ...mockDataPolicy,
      }
      cy.mountWithProviders(
        <DataHubListAction policy={policy} onEdit={cy.stub().as('onEdit')} onDelete={cy.stub().as('onDelete')} />
      )

      cy.get('button').should('have.length', 2)
      cy.getByTestId('list-action-view').should('have.attr', 'aria-label', 'Continue on draft')
      cy.getByTestId('list-action-view').should('not.be.disabled')

      cy.get('@onDelete').should('not.have.been.called')
      cy.getByTestId('list-action-delete').should('have.attr', 'aria-label', 'Delete')
      cy.getByTestId('list-action-delete').click()
      cy.get('@onDelete').should('have.been.called')
    })
  })

  context('SCRIPT and SCHEMA', () => {
    it('should render the actions for single version', () => {
      cy.mountWithProviders(
        <DataHubListAction
          policy={undefined}
          onDownload={cy.stub().as('onDownload')}
          onDelete={cy.stub().as('onDelete')}
          onEdit={cy.stub().as('onEdit')}
        />
      )

      cy.get('button').should('have.length', 2)

      cy.get('@onDownload').should('not.have.been.called')
      cy.getByTestId('list-action-download').should('not.be.disabled')
      cy.getByTestId('list-action-download').should('have.attr', 'aria-label', 'Download')
      cy.getByTestId('list-action-download').click()
      cy.get('@onDownload').should('have.been.called')

      cy.get('@onDelete').should('not.have.been.called')
      cy.getByTestId('list-action-delete').should('have.attr', 'aria-label', 'Delete')
      cy.getByTestId('list-action-delete').click()
      cy.get('@onDelete').should('have.been.called')

      cy.getByTestId('list-action-draft').should('not.exist')
      cy.getByTestId('list-action-view-edit').should('not.exist')
    })

    it('should render the actions for multiple versions', () => {
      cy.mountWithProviders(
        <DataHubListAction
          policy={undefined}
          onDownload={cy.stub().as('onDownload')}
          onDelete={cy.stub().as('onDelete')}
          onEdit={cy.stub().as('onEdit')}
        />
      )

      cy.get('button').should('have.length', 2)

      cy.get('@onDownload').should('not.have.been.called')
      cy.getByTestId('list-action-download').should('have.attr', 'aria-label', 'Download')
      cy.getByTestId('list-action-download').click()
      cy.get('@onDownload').should('have.been.called')

      cy.get('@onDelete').should('not.have.been.called')
      cy.getByTestId('list-action-delete').should('have.attr', 'aria-label', 'Delete')
      cy.getByTestId('list-action-delete').click()
      cy.get('@onDelete').should('have.been.called')

      cy.getByTestId('list-action-draft').should('not.exist')
      cy.getByTestId('list-action-view-edit').should('not.exist')
    })
  })

  context('DATA_POLICY', () => {
    it('should render the actions for DATA_POLICY', () => {
      const policy: CombinedPolicy = {
        type: PolicyType.DATA_POLICY,
        ...mockDataPolicy,
      }
      cy.mountWithProviders(
        <DataHubListAction
          policy={policy}
          onEdit={cy.stub().as('onEdit')}
          onDownload={cy.stub().as('onDownload')}
          onDelete={cy.stub().as('onDelete')}
        />
      )

      cy.get('button').should('have.length', 3)
      cy.getByTestId('list-action-view').should('not.be.disabled')

      cy.get('@onEdit').should('not.have.been.called')
      cy.getByTestId('list-action-view').should('have.attr', 'aria-label', 'View / Edit')
      cy.getByTestId('list-action-view').click()
      cy.get('@onEdit').should('have.been.called')

      cy.get('@onDownload').should('not.have.been.called')
      cy.getByTestId('list-action-download').should('have.attr', 'aria-label', 'Download')
      cy.getByTestId('list-action-download').click()
      cy.get('@onDownload').should('have.been.called')

      cy.get('@onDelete').should('not.have.been.called')
      cy.getByTestId('list-action-delete').should('have.attr', 'aria-label', 'Delete')
      cy.getByTestId('list-action-delete').click()
      cy.get('@onDelete').should('have.been.called')
    })
  })

  context('BEHAVIOR_POLICY', () => {
    it('should render the actions for BEHAVIOR_POLICY', () => {
      const policy: CombinedPolicy = {
        type: PolicyType.BEHAVIOR_POLICY,
        ...mockBehaviorPolicy,
      }
      cy.mountWithProviders(
        <DataHubListAction
          policy={policy}
          onEdit={cy.stub().as('onEdit')}
          onDownload={cy.stub().as('onDownload')}
          onDelete={cy.stub().as('onDelete')}
        />
      )

      cy.get('button').should('have.length', 3)
      cy.getByTestId('list-action-view').should('not.be.disabled')

      cy.get('@onEdit').should('not.have.been.called')
      cy.getByTestId('list-action-view').should('have.attr', 'aria-label', 'View / Edit')
      cy.getByTestId('list-action-view').click()
      cy.get('@onEdit').should('have.been.called')

      cy.get('@onDownload').should('not.have.been.called')
      cy.getByTestId('list-action-download').should('have.attr', 'aria-label', 'Download')
      cy.getByTestId('list-action-download').click()
      cy.get('@onDownload').should('have.been.called')

      cy.get('@onDelete').should('not.have.been.called')
      cy.getByTestId('list-action-delete').should('have.attr', 'aria-label', 'Delete')
      cy.getByTestId('list-action-delete').click()
      cy.get('@onDelete').should('have.been.called')
    })
  })

  it('should be accessible', () => {
    const policy: CombinedPolicy = {
      type: PolicyType.BEHAVIOR_POLICY,
      ...mockBehaviorPolicy,
    }

    cy.injectAxe()
    cy.mountWithProviders(
      <DataHubListAction
        policy={policy}
        onEdit={cy.stub().as('onEdit')}
        onDownload={cy.stub().as('onDownload')}
        onDelete={cy.stub().as('onDelete')}
      />
    )
    cy.checkAccessibility()
    cy.percySnapshot('Component: DataHub - NodeDatahubToolbar')
  })
})
