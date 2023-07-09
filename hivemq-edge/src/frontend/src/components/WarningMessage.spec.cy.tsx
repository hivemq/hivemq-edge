/// <reference types="cypress" />
import WarningMessage from '@/components/WarningMessage.tsx'
import { Flex } from '@chakra-ui/react'

// const MOCK_TITLE = 'This is a test'
const MOCK_PROMPT = 'This is a prompt'
const MOCK_ALT = 'my image'

describe('WarningMessage', () => {
  beforeEach(() => {
    // run these tests as if in a desktop
    // browser with a 720p monitor
    cy.viewport(800, 500)
  })

  it('should renders', () => {
    cy.mountWithProviders(
      <Flex width={'96vw'} height={'96vh'} flexDirection={'column'}>
        <WarningMessage prompt={MOCK_PROMPT} alt={MOCK_ALT} />
      </Flex>
    )

    // cy.get('h2').should('contain.text', MOCK_TITLE)
    cy.get(`img[alt="${MOCK_ALT}"]`).should('exist')
  })
})
