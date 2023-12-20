import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Checkbox,
  CheckboxGroup,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerHeader,
  DrawerOverlay,
  FormControl,
  FormHelperText,
  FormLabel,
  Stack,
  VStack,
} from '@chakra-ui/react'

import { EdgeFlowOptions } from '../../types.ts'
import { useEdgeFlowContext } from '../../hooks/useEdgeFlowContext.tsx'

const WorkspaceOptionsDrawer: FC = () => {
  const { t } = useTranslation()
  const { optionDrawer, options, setOptions } = useEdgeFlowContext()
  const optionKeys: (keyof EdgeFlowOptions)[] = [
    'showTopics',
    'showStatus',
    'showGateway',
    'showHosts',
    'showMonitoringOnEdge',
  ]
  const initValues = optionKeys.filter((option) => {
    const keyOption = option as keyof EdgeFlowOptions
    return options[keyOption]
  })

  return (
    <Drawer isOpen={optionDrawer.isOpen || false} placement="right" size={'md'} onClose={optionDrawer.onClose}>
      <DrawerOverlay />
      <DrawerContent>
        <DrawerCloseButton />
        <DrawerHeader>{t('workspace.configuration.header')}</DrawerHeader>

        <DrawerBody>
          <VStack gap={4}>
            <FormControl as="fieldset" borderWidth="1px" p={2}>
              <FormLabel as="legend">{t('workspace.configuration.content.header')}</FormLabel>
              <CheckboxGroup
                colorScheme="brand"
                defaultValue={initValues}
                onChange={(options) => {
                  const newOptions = options.reduce((a, opt) => ({ ...a, [opt]: true }), {})
                  const oldOptions = optionKeys.reduce((a, opt) => ({ ...a, [opt]: false }), {})

                  setOptions((old) => ({ ...old, ...oldOptions, ...newOptions }))
                }}
              >
                <Stack direction={'column'}>
                  <Checkbox value="showTopics">{t('workspace.configuration.content.showTopics')}</Checkbox>
                  <Checkbox value="showStatus">{t('workspace.configuration.content.showStatus')}</Checkbox>
                  <Checkbox value="showMonitoringOnEdge">
                    {t('workspace.configuration.content.showMonitoringOnEdge')}
                  </Checkbox>
                  <Checkbox value="showHosts">{t('workspace.configuration.content.showHosts')}</Checkbox>
                  <Checkbox value="showGateway">{t('workspace.configuration.content.showGateway')}</Checkbox>
                </Stack>
              </CheckboxGroup>
              <FormHelperText>{t('workspace.configuration.content.prompt')}</FormHelperText>
            </FormControl>
          </VStack>
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default WorkspaceOptionsDrawer
