import { useBridgeSetup } from '@/modules/Bridges/hooks/useBridgeConfig.tsx'
import { Code } from '@chakra-ui/react'
import { FC } from 'react'

const StepperDebugger: FC = () => {
  const { bridge } = useBridgeSetup()

  return (
    <Code maxHeight={150} overflowY={'auto'} marginBlock={4} size={'sm'} fontSize={'66%'}>
      <pre>{JSON.stringify(bridge, null, 2)}</pre>
    </Code>
  )
}

export default StepperDebugger
