import { SelectTag, SelectTopic, SelectTopicFilter } from '@/components/MQTT/EntityCreatableSelect.tsx'

describe('EntityCreatableSelect', () => {
  beforeEach(() => {
    cy.viewport(450, 250)
  })

  describe('SelectTopic', () => {
    it('should render', () => {
      cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 404 })
      cy.intercept('api/v1/management/protocol-adapters/adapters', { statusCode: 404 })
      cy.intercept('/api/v1/management/bridges', { statusCode: 404 })
      cy.intercept('/api/v1/management/client/filters', { statusCode: 404 })
      cy.mountWithProviders(<SelectTopic id="my-id" value="test" onChange={() => cy.stub()} />)

      cy.get('#my-id').click()
      cy.get('div').contains('No matching topics')
    })
  })

  describe('SelectTag', () => {
    it('should render', () => {
      cy.intercept('/api/v1/management/protocol-adapters/adapters/**/tags', { statusCode: 404 })

      cy.mountWithProviders(<SelectTag adapterId="my-as" id="my-id" value="test" onChange={() => cy.stub()} />)

      cy.get('#my-id').click()
      cy.get('div').contains('No matching tags')
    })
  })

  describe('SelectTopicFilter', () => {
    it('should render', () => {
      cy.intercept('/api/v1/management/topic-filters', { statusCode: 404 })

      cy.mountWithProviders(<SelectTopicFilter id="my-id" value="test" onChange={() => cy.stub()} />)

      cy.get('#my-id > div > svg').should('have.attr', 'data-option-type', 'TOPIC_FILTER')
      cy.get('#my-id > div > div').eq(0).should('have.text', 'test')

      cy.get('#my-id').click()
      cy.get('#react-select-TOPIC_FILTER-listbox').contains('No matching topic filters')
    })
  })
})
