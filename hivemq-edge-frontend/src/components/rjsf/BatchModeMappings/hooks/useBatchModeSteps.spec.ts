import { expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'

import '@/config/i18n.config.ts'

import type { BatchModeSteps, BatchModeStore } from '@/components/rjsf/BatchModeMappings/types.ts'
import { BatchModeStepType } from '@/components/rjsf/BatchModeMappings/types.ts'
import { useBatchModeSteps } from '@/components/rjsf/BatchModeMappings/hooks/useBatchModeSteps.ts'
import { MOCK_ID_SCHEMA } from '@/components/rjsf/BatchModeMappings/__test-utils__/store.mocks.ts'

const MOCK_STORE: BatchModeStore = {
  idSchema: MOCK_ID_SCHEMA,
  schema: {},
}

describe('useBatchModeSteps', () => {
  it('should initialise the stepper', async () => {
    const { result } = renderHook(() => useBatchModeSteps(MOCK_ID_SCHEMA, MOCK_STORE.schema))
    expect(result.current.isActiveStep(BatchModeStepType.UPLOAD)).toBeTruthy()
    expect(result.current.isStepCompleted(BatchModeStepType.UPLOAD)).toBeFalsy()
    expect(result.current.isStepCompleted(BatchModeStepType.MATCH)).toBeFalsy()
    expect(result.current.isStepCompleted(BatchModeStepType.VALIDATE)).toBeFalsy()
    expect(result.current.isStepCompleted(BatchModeStepType.CONFIRM)).toBeFalsy()
  })

  it('should create the steps', async () => {
    const { result } = renderHook(() => useBatchModeSteps(MOCK_ID_SCHEMA, MOCK_STORE.schema))
    expect(result.current.steps).toHaveLength(4)
    expect(result.current.steps.map((step) => step.id)).toEqual([
      BatchModeStepType.UPLOAD,
      BatchModeStepType.MATCH,
      BatchModeStepType.VALIDATE,
      BatchModeStepType.CONFIRM,
    ])
    expect(result.current.steps[3]).toEqual(expect.objectContaining<Partial<BatchModeSteps>>({ isFinal: true }))
  })

  it('should navigate between steps', async () => {
    const { result } = renderHook(() => useBatchModeSteps(MOCK_ID_SCHEMA, MOCK_STORE.schema))
    expect(result.current.isActiveStep(BatchModeStepType.UPLOAD)).toBeTruthy()
    expect(result.current.isStepCompleted(BatchModeStepType.UPLOAD)).toBeFalsy()
    act(() => {
      result.current.goToNext()
    })
    expect(result.current.isActiveStep(BatchModeStepType.MATCH)).toBeTruthy()
    act(() => {
      result.current.goToPrevious()
    })
    expect(result.current.isActiveStep(BatchModeStepType.UPLOAD)).toBeTruthy()
    act(() => {
      result.current.goToNext()
      result.current.goToNext()
    })
    expect(result.current.isActiveStep(BatchModeStepType.VALIDATE)).toBeTruthy()
    act(() => {
      result.current.goToNext()
    })
    expect(result.current.isActiveStep(BatchModeStepType.CONFIRM)).toBeTruthy()
  })

  it('should update the store', async () => {
    const { result } = renderHook(() => useBatchModeSteps(MOCK_ID_SCHEMA, MOCK_STORE.schema))

    expect(result.current.store.fileName).toBeUndefined()
    act(() => {
      result.current.onContinue({ fileName: 'a new file name' })
    })
    expect(result.current.store.fileName).toEqual('a new file name')
  })
})
