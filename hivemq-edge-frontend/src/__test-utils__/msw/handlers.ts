import { handlerCapabilities, MOCK_CAPABILITIES } from '@/api/hooks/useFrontendServices/__handlers__'
import { handlers as pulseHandlers } from '@/api/hooks/usePulse/__handlers__'
import { persistHandlers } from '@/api/hooks/useAssetMapper/__handlers__'

export const createInterceptHandlers = () => {
  return [...handlerCapabilities(MOCK_CAPABILITIES), ...pulseHandlers, ...persistHandlers()]
}
