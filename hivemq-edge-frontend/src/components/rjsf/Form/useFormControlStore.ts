import { create } from 'zustand'
import type { FormControlState, FormControlStore } from '@/components/rjsf/Form/types.ts'

const initialState: FormControlState = {
  tabIndex: 0,
  expandItems: [],
}

export const useFormControlStore = create<FormControlStore>()((set) => ({
  ...initialState,
  reset: () => {
    set(initialState)
  },
  setTabIndex: (n: number) => set(() => ({ tabIndex: n })),
  setExpandItems: (items: string[]) => set(() => ({ expandItems: items })),
}))
