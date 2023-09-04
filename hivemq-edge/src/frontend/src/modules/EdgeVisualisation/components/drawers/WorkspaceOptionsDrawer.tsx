import { FC } from 'react'
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
  Radio,
  RadioGroup,
  Stack,
  VStack,
} from '@chakra-ui/react'
import { Select } from 'chakra-react-select'

import { useEdgeFlowContext } from '@/modules/EdgeVisualisation/hooks/useEdgeFlowContext.tsx'
import { EdgeFlowOptions, EdgeFlowLayout } from '@/modules/EdgeVisualisation/types.ts'
import { groupingAttributes } from '@/modules/EdgeVisualisation/utils/layout-utils.ts'
import { useTranslation } from 'react-i18next'

const WorkspaceOptionsDrawer: FC = () => {
  const { t } = useTranslation()
  const { optionDrawer, options, setOptions, groups, setGroups } = useEdgeFlowContext()
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

            <FormControl as="fieldset" borderWidth="1px" p={2}>
              <FormLabel as="legend">{t('workspace.configuration.layout.header')}</FormLabel>

              <FormControl>
                <FormLabel>Layout method</FormLabel>
                <RadioGroup
                  colorScheme="brand"
                  value={groups.layout}
                  onChange={(v) => setGroups((old) => ({ ...old, layout: v as EdgeFlowLayout }))}
                >
                  <Stack direction="row">
                    <Radio value={EdgeFlowLayout.HORIZONTAL}>Horizontal</Radio>
                    <Radio value={EdgeFlowLayout.CIRCLE_PACKING}>Cluster</Radio>
                  </Stack>
                </RadioGroup>
              </FormControl>

              <FormControl mt={4} isDisabled={groups.layout !== EdgeFlowLayout.CIRCLE_PACKING}>
                <FormLabel>Clustering</FormLabel>
                <Select
                  colorScheme="brand"
                  isMulti
                  name="adapters"
                  defaultValue={groups.keys.map((e) => ({
                    value: e,
                    label: t('workspace.grouping.by', { context: e }) as string,
                  }))}
                  options={groupingAttributes.map((e) => ({
                    value: e.key,
                    label: t('workspace.grouping.by', { context: e.key }) as string,
                  }))}
                  onChange={(v) => setGroups((old) => ({ ...old, keys: v.map((e) => e.value) }))}
                  placeholder="Select some keys"
                  closeMenuOnSelect={true}
                />
                <FormHelperText>{t('workspace.configuration.layout.prompt')}</FormHelperText>
              </FormControl>

              <Checkbox
                mt={4}
                isDisabled={groups.layout !== EdgeFlowLayout.CIRCLE_PACKING}
                colorScheme="brand"
                isChecked={groups.showGroups}
                onChange={(v) => setGroups((old) => ({ ...old, showGroups: v.target.checked }))}
              >
                Show clusters as node
              </Checkbox>
            </FormControl>
          </VStack>
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default WorkspaceOptionsDrawer
