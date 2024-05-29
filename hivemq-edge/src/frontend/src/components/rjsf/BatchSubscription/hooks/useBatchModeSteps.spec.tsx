/// <reference types="cypress" />
import { expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'

import '@/config/i18n.config.ts'

import { BatchModeStep, BatchModeSteps } from '@/components/rjsf/BatchSubscription/types.ts'
import { useBatchModeSteps } from '@/components/rjsf/BatchSubscription/hooks/useBatchModeSteps.tsx'

describe('useBatchModeSteps', () => {
  it('should initialise the stepper', async () => {
    const { result } = renderHook(() => useBatchModeSteps())
    expect(result.current.isActiveStep(BatchModeStep.UPLOAD)).toBeTruthy()
    expect(result.current.isStepCompleted(BatchModeStep.UPLOAD)).toBeFalsy()
    expect(result.current.isStepCompleted(BatchModeStep.MATCH)).toBeFalsy()
    expect(result.current.isStepCompleted(BatchModeStep.VALIDATE)).toBeFalsy()
  })

  it('should create the steps', async () => {
    const { result } = renderHook(() => useBatchModeSteps())
    expect(result.current.steps).toHaveLength(4)
    expect(result.current.steps.map((step) => step.id)).toEqual([
      BatchModeStep.UPLOAD,
      BatchModeStep.MATCH,
      BatchModeStep.VALIDATE,
      BatchModeStep.CONFIRM,
    ])
    expect(result.current.steps[3]).toEqual(expect.objectContaining<Partial<BatchModeSteps>>({ isFinal: true }))
  })

  it('should navigate between steps', async () => {
    const { result } = renderHook(() => useBatchModeSteps())
    expect(result.current.isActiveStep(BatchModeStep.UPLOAD)).toBeTruthy()
    expect(result.current.isStepCompleted(BatchModeStep.UPLOAD)).toBeFalsy()
    act(() => {
      result.current.goToNext()
    })
    expect(result.current.isActiveStep(BatchModeStep.MATCH)).toBeTruthy()
    act(() => {
      result.current.goToPrevious()
    })
    expect(result.current.isActiveStep(BatchModeStep.UPLOAD)).toBeTruthy()
    act(() => {
      result.current.goToNext()
      result.current.goToNext()
    })
    expect(result.current.isActiveStep(BatchModeStep.VALIDATE)).toBeTruthy()
    act(() => {
      result.current.goToNext()
    })
    expect(result.current.isActiveStep(BatchModeStep.CONFIRM)).toBeTruthy()
  })
})
