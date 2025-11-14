import { ProtocolAdaptersContext } from '@/modules/Workspace/components/wizard/hooks/ProtocolAdaptersContext.tsx'
import { useContext } from 'react'

export const useProtocolAdaptersContext = () => {
  return useContext(ProtocolAdaptersContext)
}
