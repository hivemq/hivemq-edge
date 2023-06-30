import { FC } from 'react'
import { SubmitHandler, useForm } from 'react-hook-form'

import { Bridge } from '@/api/__generated__'

import ConnectionPanel from '@/modules/Bridges/components/panels/ConnectionPanel.tsx'
import { useBridgeSetup } from '../../hooks/useBridgeConfig.tsx'

interface BridgeConnectionSetupProps {
  name?: string
}

const ConnectionStep: FC<BridgeConnectionSetupProps> = () => {
  const { bridge, setBridge } = useBridgeSetup()
  const form = useForm<Bridge>({
    mode: 'onBlur',
    defaultValues: bridge,
  })
  const onSubmit: SubmitHandler<Bridge> = (data) => {
    const { host, port, username, password, clientId } = data
    setBridge((old) => {
      return { ...old, host, port, username, password, clientId }
    })
  }

  return (
    <form onSubmit={form.handleSubmit(onSubmit)} /*onChange={handleSubmit}*/>
      <ConnectionPanel form={form} />
    </form>
  )
}

export default ConnectionStep
