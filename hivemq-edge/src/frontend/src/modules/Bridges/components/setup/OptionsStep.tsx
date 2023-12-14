import { Bridge } from '@/api/__generated__'
import OptionsPanel from '@/modules/Bridges/components/panels/OptionsPanel.tsx'
import { useBridgeSetup } from '@/modules/Bridges/hooks/useBridgeConfig.tsx'
import { Flex } from '@chakra-ui/react'
import { FC } from 'react'
import { SubmitHandler, useForm } from 'react-hook-form'

const OptionsStep: FC = () => {
  const { bridge, setBridge } = useBridgeSetup()
  const form = useForm<Bridge>({
    defaultValues: bridge,
  })

  const onSubmit: SubmitHandler<Bridge> = (data) => {
    const { cleanStart, loopPreventionEnabled, loopPreventionHopCount } = data
    setBridge((old) => {
      return {
        ...old,
        cleanStart: cleanStart !== null,
        loopPreventionEnabled: loopPreventionEnabled !== null,
        loopPreventionHopCount: Number(loopPreventionHopCount),
      }
    })
  }

  return (
    <form onSubmit={form.handleSubmit(onSubmit)}>
      <Flex flexDirection={'column'} w={'80%'} m={'auto'} mt={8} maxW={600} gap={4}>
        <OptionsPanel form={form} />
      </Flex>
    </form>
  )
}

export default OptionsStep
