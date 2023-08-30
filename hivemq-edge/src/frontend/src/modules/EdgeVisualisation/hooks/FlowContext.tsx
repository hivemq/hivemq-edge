import { createContext, Dispatch, FunctionComponent, PropsWithChildren, SetStateAction, useState } from 'react'
import { EdgeFlowOptions } from '@/modules/EdgeVisualisation/types.ts'

export interface EdgeFlowContextType {
  options: EdgeFlowOptions
  setOptions: Dispatch<SetStateAction<EdgeFlowOptions>>
}

const defaultEdgeFlowContext: EdgeFlowOptions = {
  showTopics: true,
  showStatus: true,
  showMetrics: false,
  showGateway: true,
  showHosts: true,
}

export const EdgeFlowContext = createContext<EdgeFlowContextType | null>(null)

export const EdgeFlowProvider: FunctionComponent<PropsWithChildren<{ defaults?: Partial<EdgeFlowOptions> }>> = ({
  children,
  defaults,
}) => {
  const [options, setOptions] = useState<EdgeFlowOptions>({ ...defaultEdgeFlowContext, ...defaults })

  return <EdgeFlowContext.Provider value={{ options, setOptions }}>{children}</EdgeFlowContext.Provider>
}
