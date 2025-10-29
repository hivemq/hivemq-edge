/**
 * Layout Options Drawer
 *
 * Slide-out drawer for fine-tuning layout algorithm parameters.
 * Shows different options based on selected algorithm.
 */

import type { FC } from 'react'
import {
  Drawer,
  DrawerBody,
  DrawerHeader,
  DrawerOverlay,
  DrawerContent,
  DrawerCloseButton,
  VStack,
  FormControl,
  FormLabel,
  FormHelperText,
  NumberInput,
  NumberInputField,
  NumberInputStepper,
  NumberIncrementStepper,
  NumberDecrementStepper,
  Switch,
  Select,
  Divider,
  Text,
  Badge,
} from '@chakra-ui/react'
import { LayoutType } from '../../types/layout.ts'
import type { DagreOptions, RadialOptions, ColaForceOptions, ColaConstrainedOptions } from '../../types/layout.ts'

interface LayoutOptionsDrawerProps {
  isOpen: boolean
  onClose: () => void
  algorithmType: LayoutType | null
  options: Record<string, unknown>
  onOptionsChange: (options: Record<string, unknown>) => void
}

const LayoutOptionsDrawer: FC<LayoutOptionsDrawerProps> = ({
  isOpen,
  onClose,
  algorithmType,
  options,
  onOptionsChange,
}) => {
  const handleNumberChange = (key: string) => (_valueAsString: string, valueAsNumber: number) => {
    onOptionsChange({
      ...options,
      [key]: Number.isNaN(valueAsNumber) ? 0 : valueAsNumber,
    })
  }

  const handleBooleanChange = (key: string) => (e: React.ChangeEvent<HTMLInputElement>) => {
    onOptionsChange({
      ...options,
      [key]: e.target.checked,
    })
  }

  const handleSelectChange = (key: string) => (e: React.ChangeEvent<HTMLSelectElement>) => {
    onOptionsChange({
      ...options,
      [key]: e.target.value,
    })
  }

  const renderDagreOptions = () => {
    const dagreOpts = options as Partial<DagreOptions>
    return (
      <VStack spacing={4} align="stretch">
        <Text fontSize="sm" color="gray.600" _dark={{ color: 'gray.400' }}>
          Dagre hierarchical layout options
        </Text>

        <FormControl>
          <FormLabel>Rank Separation (px)</FormLabel>
          <NumberInput
            value={dagreOpts.ranksep ?? 150}
            min={50}
            max={500}
            step={10}
            onChange={handleNumberChange('ranksep')}
          >
            <NumberInputField />
            <NumberInputStepper>
              <NumberIncrementStepper />
              <NumberDecrementStepper />
            </NumberInputStepper>
          </NumberInput>
          <FormHelperText>Vertical space between ranks/layers</FormHelperText>
        </FormControl>

        <FormControl>
          <FormLabel>Node Separation (px)</FormLabel>
          <NumberInput
            value={dagreOpts.nodesep ?? 80}
            min={20}
            max={200}
            step={10}
            onChange={handleNumberChange('nodesep')}
          >
            <NumberInputField />
            <NumberInputStepper>
              <NumberIncrementStepper />
              <NumberDecrementStepper />
            </NumberInputStepper>
          </NumberInput>
          <FormHelperText>Horizontal space between nodes in same rank</FormHelperText>
        </FormControl>

        <FormControl>
          <FormLabel>Edge Separation (px)</FormLabel>
          <NumberInput
            value={dagreOpts.edgesep ?? 20}
            min={10}
            max={100}
            step={5}
            onChange={handleNumberChange('edgesep')}
          >
            <NumberInputField />
            <NumberInputStepper>
              <NumberIncrementStepper />
              <NumberDecrementStepper />
            </NumberInputStepper>
          </NumberInput>
          <FormHelperText>Space between edges</FormHelperText>
        </FormControl>

        <FormControl>
          <FormLabel>Ranker Algorithm</FormLabel>
          <Select value={dagreOpts.ranker ?? 'network-simplex'} onChange={handleSelectChange('ranker')}>
            <option value="network-simplex">Network Simplex (default)</option>
            <option value="tight-tree">Tight Tree</option>
            <option value="longest-path">Longest Path</option>
          </Select>
          <FormHelperText>Algorithm for rank assignment</FormHelperText>
        </FormControl>

        <Divider />

        <FormControl display="flex" alignItems="center">
          <FormLabel mb="0">Enable Animation</FormLabel>
          <Switch isChecked={dagreOpts.animate ?? true} onChange={handleBooleanChange('animate')} />
        </FormControl>

        <FormControl display="flex" alignItems="center">
          <FormLabel mb="0">Fit View</FormLabel>
          <Switch isChecked={dagreOpts.fitView ?? true} onChange={handleBooleanChange('fitView')} />
        </FormControl>

        {dagreOpts.animate && (
          <FormControl>
            <FormLabel>Animation Duration (ms)</FormLabel>
            <NumberInput
              value={dagreOpts.animationDuration ?? 300}
              min={0}
              max={1000}
              step={50}
              onChange={handleNumberChange('animationDuration')}
            >
              <NumberInputField />
              <NumberInputStepper>
                <NumberIncrementStepper />
                <NumberDecrementStepper />
              </NumberInputStepper>
            </NumberInput>
          </FormControl>
        )}
      </VStack>
    )
  }

  const renderRadialOptions = () => {
    const radialOpts = options as Partial<RadialOptions>
    return (
      <VStack spacing={4} align="stretch">
        <Text fontSize="sm" color="gray.600" _dark={{ color: 'gray.400' }}>
          Radial hub layout options
        </Text>

        <FormControl>
          <FormLabel>Layer Spacing (px)</FormLabel>
          <NumberInput
            value={radialOpts.layerSpacing ?? 500}
            min={200}
            max={800}
            step={50}
            onChange={handleNumberChange('layerSpacing')}
          >
            <NumberInputField />
            <NumberInputStepper>
              <NumberIncrementStepper />
              <NumberDecrementStepper />
            </NumberInputStepper>
          </NumberInput>
          <FormHelperText>Distance between concentric layers (accounts for node width)</FormHelperText>
        </FormControl>

        <FormControl>
          <FormLabel>Start Angle</FormLabel>
          <Select
            value={radialOpts.startAngle ?? -Math.PI / 2}
            onChange={(e) =>
              onOptionsChange({
                ...options,
                startAngle: Number.parseFloat(e.target.value),
              })
            }
          >
            <option value={-Math.PI / 2}>Top (12 o&apos;clock)</option>
            <option value={0}>Right (3 o&apos;clock)</option>
            <option value={Math.PI / 2}>Bottom (6 o&apos;clock)</option>
            <option value={Math.PI}>Left (9 o&apos;clock)</option>
          </Select>
          <FormHelperText>Starting position for first node</FormHelperText>
        </FormControl>

        <FormControl>
          <FormLabel>Center X (px)</FormLabel>
          <NumberInput
            value={radialOpts.centerX ?? 400}
            min={0}
            max={2000}
            step={50}
            onChange={handleNumberChange('centerX')}
          >
            <NumberInputField />
            <NumberInputStepper>
              <NumberIncrementStepper />
              <NumberDecrementStepper />
            </NumberInputStepper>
          </NumberInput>
        </FormControl>

        <FormControl>
          <FormLabel>Center Y (px)</FormLabel>
          <NumberInput
            value={radialOpts.centerY ?? 300}
            min={0}
            max={2000}
            step={50}
            onChange={handleNumberChange('centerY')}
          >
            <NumberInputField />
            <NumberInputStepper>
              <NumberIncrementStepper />
              <NumberDecrementStepper />
            </NumberInputStepper>
          </NumberInput>
        </FormControl>

        <Divider />

        <FormControl display="flex" alignItems="center">
          <FormLabel mb="0">Enable Animation</FormLabel>
          <Switch isChecked={radialOpts.animate ?? true} onChange={handleBooleanChange('animate')} />
        </FormControl>

        <FormControl display="flex" alignItems="center">
          <FormLabel mb="0">Fit View</FormLabel>
          <Switch isChecked={radialOpts.fitView ?? true} onChange={handleBooleanChange('fitView')} />
        </FormControl>

        {radialOpts.animate && (
          <FormControl>
            <FormLabel>Animation Duration (ms)</FormLabel>
            <NumberInput
              value={radialOpts.animationDuration ?? 300}
              min={0}
              max={1000}
              step={50}
              onChange={handleNumberChange('animationDuration')}
            >
              <NumberInputField />
              <NumberInputStepper>
                <NumberIncrementStepper />
                <NumberDecrementStepper />
              </NumberInputStepper>
            </NumberInput>
          </FormControl>
        )}
      </VStack>
    )
  }

  const renderColaForceOptions = () => {
    const colaOpts = options as Partial<ColaForceOptions>
    return (
      <VStack spacing={4} align="stretch">
        <Text fontSize="sm" color="gray.600" _dark={{ color: 'gray.400' }}>
          Force-directed layout options
        </Text>

        <FormControl>
          <FormLabel>Link Distance (px)</FormLabel>
          <NumberInput
            value={colaOpts.linkDistance ?? 350}
            min={200}
            max={800}
            step={25}
            onChange={handleNumberChange('linkDistance')}
          >
            <NumberInputField />
            <NumberInputStepper>
              <NumberIncrementStepper />
              <NumberDecrementStepper />
            </NumberInputStepper>
          </NumberInput>
          <FormHelperText>Target distance between connected nodes</FormHelperText>
        </FormControl>

        <FormControl>
          <FormLabel>Max Iterations</FormLabel>
          <NumberInput
            value={colaOpts.maxIterations ?? 1000}
            min={100}
            max={5000}
            step={100}
            onChange={handleNumberChange('maxIterations')}
          >
            <NumberInputField />
            <NumberInputStepper>
              <NumberIncrementStepper />
              <NumberDecrementStepper />
            </NumberInputStepper>
          </NumberInput>
          <FormHelperText>
            Maximum simulation steps
            <Badge ml={2} colorScheme="orange">
              Higher = slower
            </Badge>
          </FormHelperText>
        </FormControl>

        <FormControl>
          <FormLabel>Convergence Threshold</FormLabel>
          <NumberInput
            value={colaOpts.convergenceThreshold ?? 0.01}
            min={0.001}
            max={0.1}
            step={0.005}
            onChange={handleNumberChange('convergenceThreshold')}
          >
            <NumberInputField />
            <NumberInputStepper>
              <NumberIncrementStepper />
              <NumberDecrementStepper />
            </NumberInputStepper>
          </NumberInput>
          <FormHelperText>Stop when changes are smaller than this</FormHelperText>
        </FormControl>

        <Divider />

        <FormControl display="flex" alignItems="center">
          <FormLabel mb="0">Avoid Overlaps</FormLabel>
          <Switch isChecked={colaOpts.avoidOverlaps ?? true} onChange={handleBooleanChange('avoidOverlaps')} />
        </FormControl>

        <FormControl display="flex" alignItems="center">
          <FormLabel mb="0">Handle Disconnected</FormLabel>
          <Switch
            isChecked={colaOpts.handleDisconnected ?? true}
            onChange={handleBooleanChange('handleDisconnected')}
          />
        </FormControl>

        <FormControl display="flex" alignItems="center">
          <FormLabel mb="0">Enable Animation</FormLabel>
          <Switch isChecked={colaOpts.animate ?? true} onChange={handleBooleanChange('animate')} />
        </FormControl>

        <FormControl display="flex" alignItems="center">
          <FormLabel mb="0">Fit View</FormLabel>
          <Switch isChecked={colaOpts.fitView ?? true} onChange={handleBooleanChange('fitView')} />
        </FormControl>

        {colaOpts.animate && (
          <FormControl>
            <FormLabel>Animation Duration (ms)</FormLabel>
            <NumberInput
              value={colaOpts.animationDuration ?? 500}
              min={0}
              max={1000}
              step={50}
              onChange={handleNumberChange('animationDuration')}
            >
              <NumberInputField />
              <NumberInputStepper>
                <NumberIncrementStepper />
                <NumberDecrementStepper />
              </NumberInputStepper>
            </NumberInput>
          </FormControl>
        )}
      </VStack>
    )
  }

  const renderColaConstrainedOptions = () => {
    const colaOpts = options as Partial<ColaConstrainedOptions>
    return (
      <VStack spacing={4} align="stretch">
        <Text fontSize="sm" color="gray.600" _dark={{ color: 'gray.400' }}>
          Hierarchical constraint layout options
        </Text>

        <FormControl>
          <FormLabel>Flow Direction</FormLabel>
          <Select value={colaOpts.flowDirection ?? 'y'} onChange={handleSelectChange('flowDirection')}>
            <option value="y">Vertical (Top to Bottom)</option>
            <option value="x">Horizontal (Left to Right)</option>
          </Select>
          <FormHelperText>Direction of hierarchy flow</FormHelperText>
        </FormControl>

        <FormControl>
          <FormLabel>Layer Gap (px)</FormLabel>
          <NumberInput
            value={colaOpts.layerGap ?? 350}
            min={200}
            max={800}
            step={25}
            onChange={handleNumberChange('layerGap')}
          >
            <NumberInputField />
            <NumberInputStepper>
              <NumberIncrementStepper />
              <NumberDecrementStepper />
            </NumberInputStepper>
          </NumberInput>
          <FormHelperText>Space between layers</FormHelperText>
        </FormControl>

        <FormControl>
          <FormLabel>Node Gap (px)</FormLabel>
          <NumberInput
            value={colaOpts.nodeGap ?? 300}
            min={200}
            max={600}
            step={25}
            onChange={handleNumberChange('nodeGap')}
          >
            <NumberInputField />
            <NumberInputStepper>
              <NumberIncrementStepper />
              <NumberDecrementStepper />
            </NumberInputStepper>
          </NumberInput>
          <FormHelperText>Space between nodes in same layer</FormHelperText>
        </FormControl>

        <Divider />

        <FormControl display="flex" alignItems="center">
          <FormLabel mb="0">Enable Animation</FormLabel>
          <Switch isChecked={colaOpts.animate ?? true} onChange={handleBooleanChange('animate')} />
        </FormControl>

        <FormControl display="flex" alignItems="center">
          <FormLabel mb="0">Fit View</FormLabel>
          <Switch isChecked={colaOpts.fitView ?? true} onChange={handleBooleanChange('fitView')} />
        </FormControl>

        {colaOpts.animate && (
          <FormControl>
            <FormLabel>Animation Duration (ms)</FormLabel>
            <NumberInput
              value={colaOpts.animationDuration ?? 300}
              min={0}
              max={1000}
              step={50}
              onChange={handleNumberChange('animationDuration')}
            >
              <NumberInputField />
              <NumberInputStepper>
                <NumberIncrementStepper />
                <NumberDecrementStepper />
              </NumberInputStepper>
            </NumberInput>
          </FormControl>
        )}
      </VStack>
    )
  }

  const renderOptions = () => {
    switch (algorithmType) {
      case LayoutType.DAGRE_TB:
      case LayoutType.DAGRE_LR:
        return renderDagreOptions()
      case LayoutType.RADIAL_HUB:
        return renderRadialOptions()
      case LayoutType.COLA_FORCE:
        return renderColaForceOptions()
      case LayoutType.COLA_CONSTRAINED:
        return renderColaConstrainedOptions()
      default:
        return (
          <Text color="gray.500" _dark={{ color: 'gray.400' }}>
            Select a layout algorithm to configure options
          </Text>
        )
    }
  }

  return (
    <Drawer isOpen={isOpen} placement="right" onClose={onClose} size="md">
      <DrawerOverlay />
      <DrawerContent>
        <DrawerCloseButton />
        <DrawerHeader>Layout Options</DrawerHeader>

        <DrawerBody>{renderOptions()}</DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default LayoutOptionsDrawer
