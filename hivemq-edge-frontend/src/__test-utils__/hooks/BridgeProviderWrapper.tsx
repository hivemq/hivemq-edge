import { BridgeProvider } from '../../modules/Bridges/hooks/BridgeProvider'

export const BridgeProviderWrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <BridgeProvider>{children}</BridgeProvider>
)
