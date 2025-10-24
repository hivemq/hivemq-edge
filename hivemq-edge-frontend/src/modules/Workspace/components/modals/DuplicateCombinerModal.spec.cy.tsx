/// <reference types="cypress" />

import { ReactFlowProvider } from '@xyflow/react'
import { Button } from '@chakra-ui/react'

import type { Combiner } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { mockCombiner } from '@/api/hooks/useCombiners/__handlers__'
import DuplicateCombinerModal from './DuplicateCombinerModal.tsx'

describe('DuplicateCombinerModal', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render the modal with combiner information', () => {
    const onClose = cy.stub().as('onClose')
    const onUseExisting = cy.stub().as('onUseExisting')
    const onCreateNew = cy.stub().as('onCreateNew')

    cy.mountWithProviders(
      <ReactFlowProvider>
        <DuplicateCombinerModal
          isOpen={true}
          onClose={onClose}
          existingCombiner={mockCombiner}
          onUseExisting={onUseExisting}
          onCreateNew={onCreateNew}
        />
      </ReactFlowProvider>
    )

    // Check modal is visible
    cy.getByTestId('duplicate-combiner-modal').should('be.visible')

    // Check title and combiner name
    cy.getByTestId('modal-title').should('contain.text', 'Possible Duplicate Combiner')
    cy.getByTestId('modal-combiner-name').should('contain.text', mockCombiner.name)

    // Check description
    cy.getByTestId('modal-description').should('be.visible')

    // Check mappings section
    cy.getByTestId('modal-mappings-label').should('contain.text', 'Existing mappings')
    cy.getByTestId('mappings-list').should('be.visible')

    // Check prompt
    cy.getByTestId('modal-prompt').should('contain.text', 'What would you like to do?')

    // Check all three buttons are present
    cy.getByTestId('modal-button-cancel').should('be.visible').and('contain.text', 'Cancel')
    cy.getByTestId('modal-button-create-new').should('be.visible').and('contain.text', 'Create New Anyway')
    cy.getByTestId('modal-button-use-existing').should('be.visible').and('contain.text', 'Use Existing')
  })

  it('should render asset mapper variant correctly', () => {
    const onClose = cy.stub().as('onClose')
    const onUseExisting = cy.stub().as('onUseExisting')
    const onCreateNew = cy.stub().as('onCreateNew')

    cy.mountWithProviders(
      <ReactFlowProvider>
        <DuplicateCombinerModal
          isOpen={true}
          onClose={onClose}
          existingCombiner={mockCombiner}
          onUseExisting={onUseExisting}
          onCreateNew={onCreateNew}
          isAssetMapper={true}
        />
      </ReactFlowProvider>
    )

    cy.getByTestId('modal-title').should('contain.text', 'Possible Duplicate Asset Mapper')
  })

  it('should call onClose when cancel button is clicked', () => {
    const onClose = cy.stub().as('onClose')
    const onUseExisting = cy.stub().as('onUseExisting')
    const onCreateNew = cy.stub().as('onCreateNew')

    cy.mountWithProviders(
      <ReactFlowProvider>
        <DuplicateCombinerModal
          isOpen={true}
          onClose={onClose}
          existingCombiner={mockCombiner}
          onUseExisting={onUseExisting}
          onCreateNew={onCreateNew}
        />
      </ReactFlowProvider>
    )

    cy.getByTestId('modal-button-cancel').click()
    cy.get('@onClose').should('have.been.calledOnce')
  })

  it('should call onClose when close button (X) is clicked', () => {
    const onClose = cy.stub().as('onClose')
    const onUseExisting = cy.stub().as('onUseExisting')
    const onCreateNew = cy.stub().as('onCreateNew')

    cy.mountWithProviders(
      <ReactFlowProvider>
        <DuplicateCombinerModal
          isOpen={true}
          onClose={onClose}
          existingCombiner={mockCombiner}
          onUseExisting={onUseExisting}
          onCreateNew={onCreateNew}
        />
      </ReactFlowProvider>
    )

    cy.getByTestId('modal-close-button').click()
    cy.get('@onClose').should('have.been.calledOnce')
  })

  it('should call onUseExisting when "Use Existing" button is clicked', () => {
    const onClose = cy.stub().as('onClose')
    const onUseExisting = cy.stub().as('onUseExisting')
    const onCreateNew = cy.stub().as('onCreateNew')

    cy.mountWithProviders(
      <ReactFlowProvider>
        <DuplicateCombinerModal
          isOpen={true}
          onClose={onClose}
          existingCombiner={mockCombiner}
          onUseExisting={onUseExisting}
          onCreateNew={onCreateNew}
        />
      </ReactFlowProvider>
    )

    cy.getByTestId('modal-button-use-existing').click()
    cy.get('@onClose').should('have.been.calledOnce')
    cy.get('@onUseExisting').should('have.been.calledOnce')
  })

  it('should call onCreateNew when "Create New Anyway" button is clicked', () => {
    const onClose = cy.stub().as('onClose')
    const onUseExisting = cy.stub().as('onUseExisting')
    const onCreateNew = cy.stub().as('onCreateNew')

    cy.mountWithProviders(
      <ReactFlowProvider>
        <DuplicateCombinerModal
          isOpen={true}
          onClose={onClose}
          existingCombiner={mockCombiner}
          onUseExisting={onUseExisting}
          onCreateNew={onCreateNew}
        />
      </ReactFlowProvider>
    )

    cy.getByTestId('modal-button-create-new').click()
    cy.get('@onClose').should('have.been.calledOnce')
    cy.get('@onCreateNew').should('have.been.calledOnce')
  })

  it('should display mappings count badge', () => {
    const combinerWithMappings: Combiner = {
      ...mockCombiner,
      mappings: {
        items: [
          {
            id: 'mapping-1',
            sources: { primary: { id: 'source-1', type: DataIdentifierReference.type.TAG } },
            destination: { topic: 'test/topic' },
            instructions: [],
          },
          {
            id: 'mapping-2',
            sources: { primary: { id: 'source-2', type: DataIdentifierReference.type.TOPIC_FILTER } },
            destination: { topic: 'test/topic2' },
            instructions: [
              {
                source: '$.field',
                destination: '$.output',
                sourceRef: { id: 'ref1', type: DataIdentifierReference.type.TAG },
              },
            ],
          },
        ],
      },
    }

    cy.mountWithProviders(
      <ReactFlowProvider>
        <DuplicateCombinerModal
          isOpen={true}
          onClose={cy.stub()}
          existingCombiner={combinerWithMappings}
          onUseExisting={cy.stub()}
          onCreateNew={cy.stub()}
        />
      </ReactFlowProvider>
    )

    cy.getByTestId('mappings-count-badge').should('contain.text', '2 mappings')
    cy.getByTestId('mapping-item-mapping-1').should('be.visible')
    cy.getByTestId('mapping-item-mapping-2').should('be.visible')
  })

  it('should show empty state when no mappings exist', () => {
    const combinerWithoutMappings: Combiner = {
      ...mockCombiner,
      mappings: { items: [] },
    }

    cy.mountWithProviders(
      <ReactFlowProvider>
        <DuplicateCombinerModal
          isOpen={true}
          onClose={cy.stub()}
          existingCombiner={combinerWithoutMappings}
          onUseExisting={cy.stub()}
          onCreateNew={cy.stub()}
        />
      </ReactFlowProvider>
    )

    cy.getByTestId('mappings-list-empty').should('be.visible').and('contain.text', 'No mappings defined yet')
  })

  it('should focus on "Use Existing" button when modal opens', () => {
    cy.mountWithProviders(
      <ReactFlowProvider>
        <DuplicateCombinerModal
          isOpen={true}
          onClose={cy.stub()}
          existingCombiner={mockCombiner}
          onUseExisting={cy.stub()}
          onCreateNew={cy.stub()}
        />
      </ReactFlowProvider>
    )

    // The primary button should receive initial focus
    cy.getByTestId('modal-button-use-existing').should('have.focus')
  })

  it('should support keyboard navigation with ESC key', () => {
    const onClose = cy.stub().as('onClose')

    cy.mountWithProviders(
      <ReactFlowProvider>
        <DuplicateCombinerModal
          isOpen={true}
          onClose={onClose}
          existingCombiner={mockCombiner}
          onUseExisting={cy.stub()}
          onCreateNew={cy.stub()}
        />
      </ReactFlowProvider>
    )

    cy.get('body').type('{esc}')
    cy.get('@onClose').should('have.been.calledOnce')
  })

  it('should not render when isOpen is false', () => {
    cy.mountWithProviders(
      <ReactFlowProvider>
        <div>
          <Button data-testid="external-button">External Content</Button>
          <DuplicateCombinerModal
            isOpen={false}
            onClose={cy.stub()}
            existingCombiner={mockCombiner}
            onUseExisting={cy.stub()}
            onCreateNew={cy.stub()}
          />
        </div>
      </ReactFlowProvider>
    )

    cy.getByTestId('external-button').should('be.visible')
    cy.getByTestId('duplicate-combiner-modal').should('not.exist')
  })

  it('should be accessible', () => {
    cy.viewport(1280, 900)
    cy.injectAxe()

    const combinerWithMappings: Combiner = {
      ...mockCombiner,
      name: 'Production Data Combiner',
      mappings: {
        items: [
          {
            id: 'mapping-1',
            sources: { primary: { id: 'temperature-sensor', type: DataIdentifierReference.type.TAG } },
            destination: { topic: 'factory/floor1/temperature' },
            instructions: [
              {
                source: '$.temp',
                destination: '$.temperature',
                sourceRef: { id: 'temp-ref', type: DataIdentifierReference.type.TAG },
              },
              {
                source: '$.unit',
                destination: '$.unit',
                sourceRef: { id: 'unit-ref', type: DataIdentifierReference.type.TAG },
              },
            ],
          },
          {
            id: 'mapping-2',
            sources: { primary: { id: 'pressure-sensor', type: DataIdentifierReference.type.TAG } },
            destination: { topic: 'factory/floor1/pressure' },
            instructions: [
              {
                source: '$.pressure',
                destination: '$.value',
                sourceRef: { id: 'pressure-ref', type: DataIdentifierReference.type.TAG },
              },
            ],
          },
        ],
      },
    }

    cy.mountWithProviders(
      <ReactFlowProvider>
        <DuplicateCombinerModal
          isOpen={true}
          onClose={cy.stub()}
          existingCombiner={combinerWithMappings}
          onUseExisting={cy.stub()}
          onCreateNew={cy.stub()}
        />
      </ReactFlowProvider>
    )

    // Check accessible state with meaningful props
    cy.getByTestId('duplicate-combiner-modal').should('be.visible')
    cy.checkAccessibility()

    // Optional: Capture screenshot for PR documentation
    cy.screenshot('duplicate-combiner-modal-accessible', {
      capture: 'fullPage',
      overwrite: true,
    })
  })
})
