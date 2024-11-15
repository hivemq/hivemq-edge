import { create } from 'zustand'
import { FormControlStore } from '@/components/rjsf/Form/types.ts'

export const useFormControlStore = create<FormControlStore>()((set) => ({
  tabIndex: 0,
  setTabIndex: (n: number) => set(() => ({ tabIndex: n })),
  clearController: () => set(() => ({ tabIndex: 0 })),
}))
