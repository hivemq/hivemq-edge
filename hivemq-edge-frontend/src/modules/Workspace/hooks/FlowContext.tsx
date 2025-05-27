import type { Dispatch, SetStateAction } from 'react'
import { createContext } from 'react'
import type { UseDisclosureReturn } from '@chakra-ui/react'

import type { EdgeFlowGrouping, EdgeFlowOptions } from '../types.ts'

export interface EdgeFlowContextType {
  options: EdgeFlowOptions
  setOptions: Dispatch<SetStateAction<EdgeFlowOptions>>
  groups: EdgeFlowGrouping
  setGroups: Dispatch<SetStateAction<EdgeFlowGrouping>>
  optionDrawer: UseDisclosureReturn
}

export const EdgeFlowContext = createContext<EdgeFlowContextType | null>(null)
