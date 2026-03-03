import { Modal, ModalCloseButton, ModalContent, ModalHeader, ModalOverlay } from '@chakra-ui/react'
import type { FC, PropsWithChildren } from 'react'
import type { DataCombining } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import type { CombinerContext } from '@/modules/Mappings/types'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils'
import SchemaMerger from './SchemaMerger'

// TODO[NVL] This is wrong: the modal should be a reusable component
const Wrapper: FC<PropsWithChildren> = ({ children }) => (
  <Modal isOpen={true} onClose={cy.stub} id="destination-schema">
    <ModalOverlay />
    <ModalContent>
      <ModalCloseButton />
      <ModalHeader>Merging</ModalHeader>
      {children}
    </ModalContent>
  </Modal>
)

const writingSchemaUrl = (adapterId: string, tagName: string) =>
  `/api/v1/management/protocol-adapters/writing-schema/${adapterId}/${encodeURIComponent(tagName)}`

describe('SchemaMerger', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render error properly', () => {
    const onUpload = cy.stub().as('onUpload')
    const onClose = cy.stub().as('onClose')
    cy.mountWithProviders(<SchemaMerger onUpload={onUpload} onClose={onClose} />, { wrapper: Wrapper })

    cy.get('[role="dialog"]#chakra-modal-destination-schema').should('be.visible')
    cy.get('[role="dialog"]#chakra-modal-destination-schema').within(() => {
      cy.get('header').should('have.text', 'Merging')
      cy.getByTestId('schema-infer-prompt').should(
        'have.text',
        'We can create a new schema based on the current state of the sources'
      )
      cy.getByTestId('schema-infer-merged').should('have.attr', 'data-invalid')
      cy.getByTestId('schema-infer-merged').within(() => {
        cy.get('label').should('contain.text', 'Suggested merged schema')
        cy.get('#schema-infer-properties-feedback').should('have.text', 'You need to define the source schemas first')
      })

      cy.get('footer').within(() => {
        cy.getByTestId('schema-infer-cancel').should('have.text', 'Cancel')
        cy.getByTestId('schema-infer-generate').should('have.text', 'Generate').should('be.disabled')

        cy.get('@onClose').should('not.have.been.called')
        cy.getByTestId('schema-infer-cancel').click()
        cy.get('@onClose').should('have.been.called')
      })
    })
  })

  describe('schema inference with same-named tags from different adapters (scope)', () => {
    const ADAPTER_A = 'adapter-a'
    const ADAPTER_B = 'adapter-b'
    const TAG_NAME = 'temperature'

    const schemaAdapterA = {
      type: 'object',
      properties: { value: { type: 'number' } },
    }

    const schemaAdapterB = {
      type: 'object',
      properties: { value: { type: 'string' } },
    }

    const formData: DataCombining = {
      id: 'test-mapping-id',
      sources: {
        primary: { id: TAG_NAME, type: DataIdentifierReference.type.TAG, scope: ADAPTER_A },
        tags: [TAG_NAME, TAG_NAME],
        topicFilters: [],
      },
      destination: { topic: 'my/topic' },
      instructions: [],
    }

    const formContext: CombinerContext = {
      selectedSources: {
        tags: [
          { id: TAG_NAME, type: DataIdentifierReference.type.TAG, scope: ADAPTER_A },
          { id: TAG_NAME, type: DataIdentifierReference.type.TAG, scope: ADAPTER_B },
        ],
        topicFilters: [],
      },
    }

    beforeEach(() => {
      cy.intercept('GET', writingSchemaUrl(ADAPTER_A, TAG_NAME), {
        statusCode: 200,
        body: schemaAdapterA,
      }).as('schemaAdapterA')

      cy.intercept('GET', writingSchemaUrl(ADAPTER_B, TAG_NAME), {
        statusCode: 200,
        body: schemaAdapterB,
      }).as('schemaAdapterB')
    })

    it('should display one property per source, with no duplicates', () => {
      cy.mountWithProviders(
        <SchemaMerger formData={formData} formContext={formContext} onUpload={cy.stub()} onClose={cy.stub()} />,
        { wrapper: Wrapper }
      )

      cy.wait(['@schemaAdapterA', '@schemaAdapterB'])

      // Two tags, each with one root property → 2 items in the merged list
      cy.getByTestId('schema-infer-merged').within(() => {
        cy.get('[data-testid="property-name"]').should('have.length', 2)

        // The two properties must have distinct display names (not both 'tg1_value')
        cy.get('[data-testid="property-name"]')
          .eq(0)
          .invoke('text')
          .then((first) => {
            cy.get('[data-testid="property-name"]').eq(1).invoke('text').should('not.eq', first)
          })
      })
    })

    it('should generate a schema with unique property keys when tags share a name across adapters', () => {
      const onUpload = cy.stub().as('onUpload')

      cy.mountWithProviders(
        <SchemaMerger formData={formData} formContext={formContext} onUpload={onUpload} onClose={cy.stub()} />,
        { wrapper: Wrapper }
      )

      cy.wait(['@schemaAdapterA', '@schemaAdapterB'])

      cy.getByTestId('schema-infer-generate').click()

      cy.get('@onUpload')
        .should('have.been.calledOnce')
        .then((stub) => {
          const properties: FlatJSONSchema7[] = (stub as unknown as sinon.SinonStub).args[0][0]
          const keys = properties.map((p) => p.key)
          // All keys must be unique — no duplicates allowed in a JSON Schema properties object
          expect(new Set(keys).size, 'all property keys are unique').to.equal(keys.length)
          // Both source schemas must contribute properties (not one overwriting the other)
          expect(keys, 'two properties generated, one per adapter').to.have.length(2)
        })
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<SchemaMerger onUpload={cy.stub} onClose={cy.stub} />, { wrapper: Wrapper })

    cy.get('label + div').click()
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] ReactSelect not tagging properly the listbox
        'aria-input-field-name': { enabled: false },
      },
    })
  })
})
