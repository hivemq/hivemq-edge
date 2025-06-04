import type { FC, PropsWithChildren } from 'react'
import { stratify } from 'd3-hierarchy'
import { Box } from '@chakra-ui/react'
import SunburstChart from '@/modules/DomainOntology/components/charts/SunburstChart.tsx'
import type { TopicTreeMetadata } from '@/modules/Workspace/types.ts'

const MOCK_TOPICS = ['test/r1', 'test/r2', 'test/r3', 'test/r4']
const MOCK_STRATIFY_TOPICS = stratify<TopicTreeMetadata>().path((d) => d.label)(
  MOCK_TOPICS.map((e) => ({ label: e, count: 1 }))
)

const Wrapper: FC<PropsWithChildren> = ({ children }) => {
  return (
    <Box h="360" overflow="hidden">
      {children}
    </Box>
  )
}

describe('SunburstChart', () => {
  beforeEach(() => {
    cy.viewport(600, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<SunburstChart data={MOCK_STRATIFY_TOPICS} />, { wrapper: Wrapper })

    cy.get('svg').as('sunburstChart')
    cy.get('@sunburstChart').find('g g:first-child > path')
    cy.get('@sunburstChart').find('g g:nth-child(2) > g').as('labels')
    cy.get('@labels').eq(0).should('contain.text', 'r1')
    cy.get('@labels').eq(1).should('contain.text', 'r2')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<SunburstChart data={MOCK_STRATIFY_TOPICS} />, { wrapper: Wrapper })

    cy.checkAccessibility()
    cy.percySnapshot('Component: SunburstChart')
  })
})
