import { createContext, Dispatch, FunctionComponent, PropsWithChildren, SetStateAction, useState } from 'react'
import { EdgeFlowOptions } from '@/modules/EdgeVisualisation/types.ts'

export interface EdgeFlowContextType {
  options: EdgeFlowOptions
  setOptions: Dispatch<SetStateAction<EdgeFlowOptions>>
}

const defaultEdgeFlowContext: EdgeFlowOptions = {
  showTopics: true,
  showMetrics: true,
}

export const EdgeFlowContext = createContext<EdgeFlowContextType | null>(null)

export const EdgeFlowProvider: FunctionComponent<PropsWithChildren> = ({ children }) => {
  const [options, setOptions] = useState<EdgeFlowOptions>(defaultEdgeFlowContext)

  return <EdgeFlowContext.Provider value={{ options, setOptions }}>{children}</EdgeFlowContext.Provider>
}
