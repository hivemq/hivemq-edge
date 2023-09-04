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
  Stack,
} from '@chakra-ui/react'
import { Select } from 'chakra-react-select'

import { useEdgeFlowContext } from '@/modules/EdgeVisualisation/hooks/useEdgeFlowContext.tsx'
import { EdgeFlowOptions } from '@/modules/EdgeVisualisation/types.ts'
import { groupingAttributes } from '@/modules/EdgeVisualisation/utils/layout-utils.ts'

const WorkspaceOptionsDrawer: FC = () => {
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
        <DrawerHeader>Workspace configuration</DrawerHeader>

        <DrawerBody>
          <FormControl as="fieldset">
            <FormLabel as="legend">Topology Graph: Content</FormLabel>
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
                <Checkbox value="showTopics">Topics</Checkbox>
                <Checkbox value="showStatus">Status</Checkbox>
                <Checkbox value="showMonitoringOnEdge">Monitoring on links</Checkbox>
                <Checkbox value="showHosts">Hosts</Checkbox>
                <Checkbox value="showGateway">Gateway</Checkbox>
              </Stack>
            </CheckboxGroup>
            <FormHelperText>Select the contents to be displayed on the workspace</FormHelperText>
          </FormControl>

          <FormControl as="fieldset" mt={4}>
            <FormLabel>Topology Graph: Adapter grouping</FormLabel>
            <Select
              isMulti
              name="adapters"
              options={groupingAttributes.map((e) => ({ value: e.key, label: e.key }))}
              onChange={(e) => console.log('XXXXX val', e)}
              placeholder="Select some keys"
              closeMenuOnSelect={true}
            />
          </FormControl>
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default WorkspaceOptionsDrawer
