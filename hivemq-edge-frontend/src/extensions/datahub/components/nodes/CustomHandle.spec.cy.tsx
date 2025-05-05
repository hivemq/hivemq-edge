/// <reference types="cypress" />

import { Position } from '@xyflow/react'

import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'

import { CustomHandle } from './CustomHandle.tsx'

describe('CustomHandle', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<CustomHandle type="target" position={Position.Bottom} />))
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<CustomHandle type="source" position={Position.Bottom} />))
  })
})
