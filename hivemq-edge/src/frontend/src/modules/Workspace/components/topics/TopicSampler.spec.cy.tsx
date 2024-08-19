import TopicSampler from '@/modules/Workspace/components/topics/TopicSampler.tsx'
import { FC, PropsWithChildren } from 'react'
import { Text, VStack } from '@chakra-ui/react'
import { useGetTopicSamples } from '@/api/hooks/useTopicOntology/useGetTopicSamples.tsx'

const Wrapper: FC<PropsWithChildren> = ({ children }) => {
  const { isLoading, data } = useGetTopicSamples()
  return (
    <VStack alignItems="flex-start">
      {children}
      <VStack alignItems="flex-start">
        <Text data-testid="sampler-loader">{isLoading ? 'loading' : 'loaded'}</Text>
        <ul data-testid="sampler-listing">
          {data?.map((e, i) => (
            <li key={`${e}-${i}`}>{e}</li>
          ))}
        </ul>
      </VStack>
    </VStack>
  )
}

describe('TopicSampler', () => {
  beforeEach(() => {
    cy.viewport(600, 600)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<TopicSampler topic="this/is/a/topic" />, { wrapper: Wrapper })

    cy.get('h2').should('contain.text', 'this/is/a/topic')
    cy.getByTestId('sampler-warning').should(
      'contain.text',
      "The device is using a wildcard to subscribe to topics, they are therefore not known during it's configuration."
    )
    cy.get('button').should('contain.text', 'Get Topic Samples')

    cy.getByTestId('sampler-loader').should('contain.text', 'loaded')
  })

  it('should load the samples', () => {
    cy.mountWithProviders(<TopicSampler topic="this/is/a/topic" />, { wrapper: Wrapper })

    cy.getByTestId('sampler-loader').should('contain.text', 'loaded')
    cy.get('button').click()
    cy.get('button')
      .should('contain.text', 'Monitoring MQTT traffic')
      .should('have.attr', 'disabled', 'disabled')
      .should('have.attr', 'data-loading')

    cy.getByTestId('sampler-listing').find('li').should('have.length', 5)
    cy.getByTestId('sampler-listing').find('li').eq(0).should('have.text', 'tmp/broker1/topic1/segment1')
    cy.getByTestId('sampler-listing').find('li').eq(4).should('have.text', 'tmp/broker4/topic1/segment2')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TopicSampler topic="this/is/a/topic" />)

    cy.checkAccessibility()
    cy.percySnapshot('Component: TopicSampler')
  })
})
