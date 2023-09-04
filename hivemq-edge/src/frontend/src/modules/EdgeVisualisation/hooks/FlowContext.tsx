import { createContext, Dispatch, FunctionComponent, PropsWithChildren, SetStateAction, useState } from 'react'
import { useDisclosure, UseDisclosureReturn } from '@chakra-ui/react'

import { EdgeFlowGrouping, EdgeFlowOptions } from '../types.ts'

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
  showMetrics: false,
  showGateway: false,
  showHosts: false,
  showObservabilityEdge: false,
}

const defaultEdgeFloeGrouping: EdgeFlowGrouping = {
  keys: [],
  showGroups: true,
}

export const EdgeFlowContext = createContext<EdgeFlowContextType | null>(null)

export const EdgeFlowProvider: FunctionComponent<PropsWithChildren<{ defaults?: Partial<EdgeFlowOptions> }>> = ({
  children,
  defaults,
}) => {
  const [options, setOptions] = useState<EdgeFlowOptions>({ ...defaultEdgeFlowContext, ...defaults })
  const [groups, setGroups] = useState<EdgeFlowGrouping>(defaultEdgeFloeGrouping)
  const optionDrawer = useDisclosure()

  return (
    <EdgeFlowContext.Provider value={{ options, setOptions, groups, setGroups, optionDrawer }}>
      {children}
    </EdgeFlowContext.Provider>
  )
}
