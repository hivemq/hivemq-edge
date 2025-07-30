import { handlerCapabilities, MOCK_CAPABILITIES } from '@/api/hooks/useFrontendServices/__handlers__'

export const createInterceptHandlers = () => [...handlerCapabilities(MOCK_CAPABILITIES)]
