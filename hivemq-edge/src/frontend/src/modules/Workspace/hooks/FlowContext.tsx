import type { Dispatch, FunctionComponent, PropsWithChildren, SetStateAction } from 'react'
import { createContext, useState } from 'react'
import type { UseDisclosureReturn } from '@chakra-ui/react'
import { useDisclosure } from '@chakra-ui/react'

import type { EdgeFlowGrouping, EdgeFlowOptions } from '../types.ts'
import { EdgeFlowLayout } from '../types.ts'

export interface EdgeFlowContextType {
  options: EdgeFlowOptions
  setOptions: Dispatch<SetStateAction<EdgeFlowOptions>>
  groups: EdgeFlowGrouping
  setGroups: Dispatch<SetStateAction<EdgeFlowGrouping>>
  optionDrawer: UseDisclosureReturn
}

const defaultEdgeFlowContext: EdgeFlowOptions = {
  showTopics: true,
  showStatus: true,
  showGateway: false,
}

const defaultEdgeFlowGrouping: EdgeFlowGrouping = {
  keys: [],
  showGroups: false,
  layout: EdgeFlowLayout.HORIZONTAL,
}

export const EdgeFlowContext = createContext<EdgeFlowContextType | null>(null)

export const EdgeFlowProvider: FunctionComponent<PropsWithChildren<{ defaults?: Partial<EdgeFlowOptions> }>> = ({
  children,
  defaults,
}) => {
  const [options, setOptions] = useState<EdgeFlowOptions>({ ...defaultEdgeFlowContext, ...defaults })
  const [groups, setGroups] = useState<EdgeFlowGrouping>(defaultEdgeFlowGrouping)
  const optionDrawer = useDisclosure()

  return (
    <EdgeFlowContext.Provider value={{ options, setOptions, groups, setGroups, optionDrawer }}>
      {children}
    </EdgeFlowContext.Provider>
  )
}
