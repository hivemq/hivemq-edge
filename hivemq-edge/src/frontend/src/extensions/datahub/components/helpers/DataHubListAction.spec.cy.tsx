/// <reference types="cypress" />

import DataHubListAction from '@datahub/components/helpers/DataHubListAction.tsx'
import { CombinedPolicy, PolicyType } from '@datahub/types.ts'
import { mockDataPolicy } from '@datahub/api/hooks/DataHubDataPoliciesService/__handlers__'
import { mockBehaviorPolicy } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/__handlers__'

describe('DataHubListAction', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the actions for DRAFT', () => {
    const policy: CombinedPolicy = {
      type: PolicyType.CREATE_POLICY,
      ...mockDataPolicy,
    }
    cy.mountWithProviders(
      <DataHubListAction policy={policy} onEdit={cy.stub().as('onEdit')} onDelete={cy.stub().as('onDelete')} />
    )

    cy.get('button').should('have.length', 2)
    cy.getByTestId('list-action-view').should('not.be.disabled')

    cy.get('@onDelete').should('not.have.been.called')
    cy.getByTestId('list-action-delete').click()
    cy.get('@onDelete').should('have.been.called')
  })

  it('should render the actions for resources', () => {
    cy.mountWithProviders(
      <DataHubListAction
        policy={undefined}
        onDownload={cy.stub().as('onDownload')}
        onDelete={cy.stub().as('onDelete')}
      />
    )

    cy.get('button').should('have.length', 2)
    cy.getByTestId('list-action-download').should('not.be.disabled')

    cy.get('@onDownload').should('not.have.been.called')
    cy.getByTestId('list-action-download').click()
    cy.get('@onDownload').should('have.been.called')

    cy.get('@onDelete').should('not.have.been.called')
    cy.getByTestId('list-action-delete').click()
    cy.get('@onDelete').should('have.been.called')
  })

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

    cy.get('button').should('have.length', 4)
    cy.getByTestId('list-action-view').should('not.be.disabled')

    cy.get('@onEdit').should('not.have.been.called')
    cy.getByTestId('list-action-view').click()
    cy.get('@onEdit').should('have.been.called')

    cy.get('@onDownload').should('not.have.been.called')
    cy.getByTestId('list-action-download').click()
    cy.get('@onDownload').should('have.been.called')

    cy.get('@onDelete').should('not.have.been.called')
    cy.getByTestId('list-action-delete').click()
    cy.get('@onDelete').should('have.been.called')
  })

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

    cy.get('button').should('have.length', 4)
    cy.getByTestId('list-action-view').should('not.be.disabled')

    cy.get('@onEdit').should('not.have.been.called')
    cy.getByTestId('list-action-view').click()
    cy.get('@onEdit').should('have.been.called')

    cy.get('@onDownload').should('not.have.been.called')
    cy.getByTestId('list-action-download').click()
    cy.get('@onDownload').should('have.been.called')

    cy.get('@onDelete').should('not.have.been.called')
    cy.getByTestId('list-action-delete').click()
    cy.get('@onDelete').should('have.been.called')
  })
})
