/// <reference types="cypress" />

import { FC } from 'react'
import { useForm } from 'react-hook-form'

import { Bridge } from '@/api/__generated__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_CAPABILITY_PERSISTENCE } from '@/api/hooks/useFrontendServices/__handlers__'

import PersistencePanel from './PersistencePanel.tsx'
import { Button } from '@chakra-ui/react'

interface TestingComponentProps {
  onSubmit: (data: Bridge) => void
  defaultValues: Bridge
}

const TestingComponent: FC<TestingComponentProps> = ({ onSubmit, defaultValues }) => {
  const form = useForm<Bridge>({
    mode: 'all',
    criteriaMode: 'all',
    defaultValues: defaultValues,
  })
  return (
    <div>
      <form id="bridge-form" onSubmit={form.handleSubmit(onSubmit)}>
        <PersistencePanel form={form} hasPersistence={MOCK_CAPABILITY_PERSISTENCE} />
      </form>
      <Button variant={'primary'} type={'submit'} form="bridge-form" data-testid={'form-submit'} mt={8}>
        Submit
      </Button>
    </div>
  )
}

describe('PersistencePanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TestingComponent onSubmit={cy.stub} defaultValues={mockBridge} />)

    cy.get('#field-\\:r1\\:-helptext')
      .should('be.visible')
      .should('contain.text', 'Select to store MQTT Traffic greater than QoS 0 on disk.')
    cy.get('label').should('contain.text', 'MQTT Persistence')

    cy.checkAccessibility()
    cy.percySnapshot('Component: PersistencePanel')
  })
})
