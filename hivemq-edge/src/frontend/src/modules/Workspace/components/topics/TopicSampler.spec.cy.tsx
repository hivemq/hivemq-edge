import TopicSampler from '@/modules/Workspace/components/topics/TopicSampler.tsx'
import { FC, PropsWithChildren } from 'react'
import { Text, VStack } from '@chakra-ui/react'
import { useGetClientTopicSamples } from '@/api/hooks/useClientSubscriptions/useGetClientTopicSamples.ts'
import { MOCK_MQTT_TOPIC_SAMPLES } from '@/api/hooks/useClientSubscriptions/__handlers__'

const Wrapper: FC<PropsWithChildren> = ({ children }) => {
  const { isLoading, data } = useGetClientTopicSamples()
  return (
    <VStack alignItems="flex-start">
      {children}
      <VStack alignItems="flex-start">
        <Text data-testid="sampler-loader">{isLoading ? 'loading' : 'loaded'}</Text>
        <ul data-testid="sampler-listing">
          {data?.items?.map((topic, index) => (
            <li key={`${topic}-${index}`}>{topic}</li>
          ))}
        </ul>
      </VStack>
    </VStack>
  )
}

describe('TopicSampler', () => {
  beforeEach(() => {
    cy.viewport(600, 600)
    cy.intercept(
      {
        url: '/api/v1/**',
        middleware: true,
      },
      (req) => {
        req.on('response', (res) => {
          // Throttle the response to 1 Mbps to simulate a
          // mobile 3G connection
          console.log('SXXXXX')
          res.setThrottle(5000)
        })
      }
    )
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
    cy.intercept('/api/v1/management/client/topic-samples*', { items: MOCK_MQTT_TOPIC_SAMPLES }).as('getSamples')
    cy.mountWithProviders(<TopicSampler topic="this/is/a/topic" />, { wrapper: Wrapper })

    cy.getByTestId('sampler-loader').should('contain.text', 'loaded')
    cy.get('button').click()
    cy.wait('@getSamples')

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
