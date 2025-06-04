import { expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'

import { useFormControlStore } from '@/components/rjsf/Form/useFormControlStore.ts'
import type { FormControlStore } from '@/components/rjsf/Form/types.ts'

describe('useWorkspaceStore', () => {
  beforeEach(() => {
    const { result } = renderHook<FormControlStore, undefined>(useFormControlStore)
    act(() => {
      result.current.reset()
    })
  })

  it('should start with a default store', async () => {
    const { result } = renderHook<FormControlStore, undefined>(useFormControlStore)

    expect(result.current.expandItems).toStrictEqual([])
    expect(result.current.tabIndex).toStrictEqual(0)
  })

  it('should change the selected tab', async () => {
    const { result } = renderHook<FormControlStore, undefined>(useFormControlStore)

    expect(result.current.expandItems).toStrictEqual([])
    expect(result.current.tabIndex).toStrictEqual(0)

    act(() => {
      const { setTabIndex } = result.current
      setTabIndex(2)
    })

    expect(result.current.expandItems).toStrictEqual([])
    expect(result.current.tabIndex).toStrictEqual(2)
  })

  it('should change the list of collapsed items', async () => {
    const { result } = renderHook<FormControlStore, undefined>(useFormControlStore)

    expect(result.current.expandItems).toStrictEqual([])
    expect(result.current.tabIndex).toStrictEqual(0)

    act(() => {
      const { setExpandItems } = result.current
      setExpandItems(['first', 'second'])
    })

    expect(result.current.expandItems).toStrictEqual(['first', 'second'])
    expect(result.current.tabIndex).toStrictEqual(0)
  })

  it('should reset the store', async () => {
    const { result } = renderHook<FormControlStore, undefined>(useFormControlStore)

    expect(result.current.expandItems).toStrictEqual([])
    expect(result.current.tabIndex).toStrictEqual(0)

    act(() => {
      const { setExpandItems, setTabIndex } = result.current
      setExpandItems(['first', 'second'])
      setTabIndex(2)
    })

    expect(result.current.expandItems).toStrictEqual(['first', 'second'])
    expect(result.current.tabIndex).toStrictEqual(2)

    act(() => {
      const { reset } = result.current
      reset()
    })

    expect(result.current.expandItems).toStrictEqual([])
    expect(result.current.tabIndex).toStrictEqual(0)
  })
})
