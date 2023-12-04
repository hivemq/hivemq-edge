/// <reference types="cypress" />

import { FC } from 'react'
import { useForm } from 'react-hook-form'

import { Bridge } from '@/api/__generated__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_CAPABILITY_PERSISTENCE } from '@/api/hooks/useFrontendServices/__handlers__'
import ButtonCTA from '@/components/Chakra/ButtonCTA.tsx'

import PersistencePanel from './PersistencePanel.tsx'

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
      <ButtonCTA type={'submit'} form="bridge-form" data-testid={'form-submit'} mt={8}>
        Submit
      </ButtonCTA>
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
      .should(
        'contain.text',
        'Mqtt Traffic with QoS greater than 0 will be stored persistently on disc and loaded on restart of Edge'
      )
    cy.get('label').should('contain.text', 'Persist Data for MQTT traffic')
    cy.checkAccessibility()
    cy.percySnapshot('Component: PersistencePanel')
  })
})
