import type { FC } from 'react'
import { useMemo } from 'react'
import { useTheme, useColorMode } from '@chakra-ui/react'

import { Mermaid } from '@/components/Mermaid.tsx'

import type { FiniteStateMachine, FsmState, FsmTransition } from '@datahub/types.ts'

export interface MermaidRendererProps extends FiniteStateMachine {
  selectedTransition?: {
    event: string
    from: string
    to: string
  }
}

export const MermaidRenderer: FC<MermaidRendererProps> = (props) => {
  const theme = useTheme()
  const { colorMode } = useColorMode()

  const script = useMemo(() => {
    console.log('MermaidRenderer render - props:', {
      states: props.states,
      transitions: props.transitions,
      selectedTransition: props.selectedTransition,
      colorMode,
    })

    if (!props.states || !props.transitions) {
      console.log('MermaidRenderer - missing states or transitions')
      return null
    }

    // Get Chakra UI theme colors for current mode
    const colors = theme.colors

    // Helper to resolve color token to hex
    const resolveColor = (colorPath: string): string => {
      const parts = colorPath.split('.')
      let value: unknown = colors

      for (const part of parts) {
        if (value && typeof value === 'object' && part in value) {
          value = (value as Record<string, unknown>)[part]
        } else {
          value = undefined
          break
        }
      }

      // If it's an object with _light/_dark, use colorMode
      if (typeof value === 'object' && value !== null) {
        const modeKey = colorMode === 'dark' ? '_dark' : '_light'
        const modeValue = (value as Record<string, unknown>)[modeKey]
        if (typeof modeValue === 'string') {
          return resolveColor(modeValue.replace('colors.', ''))
        }
      }

      return typeof value === 'string' ? value : '#000000'
    }

    // Get Badge colors (matching TransitionSelect - SUBTLE variant is default)
    const getBadgeColors = (colorScheme: string) => {
      const isDark = colorMode === 'dark'

      switch (colorScheme) {
        case 'blue':
          return {
            // Subtle variant: light bg with dark text (light mode), transparent bg with light text (dark mode)
            fill: isDark ? 'rgba(144, 205, 244, 0.16)' : resolveColor('blue.100'),
            stroke: isDark ? resolveColor('blue.300') : resolveColor('blue.200'),
            text: isDark ? resolveColor('blue.200') : resolveColor('blue.800'),
          }
        case 'gray':
          return {
            fill: isDark ? 'rgba(237, 242, 247, 0.16)' : resolveColor('gray.100'),
            stroke: isDark ? resolveColor('gray.300') : resolveColor('gray.200'),
            text: isDark ? resolveColor('gray.200') : resolveColor('gray.800'),
          }
        case 'green':
          return {
            fill: isDark ? 'rgba(154, 230, 180, 0.16)' : resolveColor('green.100'),
            stroke: isDark ? resolveColor('green.300') : resolveColor('green.200'),
            text: isDark ? resolveColor('green.200') : resolveColor('green.800'),
          }
        case 'red':
          return {
            fill: isDark ? 'rgba(254, 178, 178, 0.16)' : resolveColor('red.100'),
            stroke: isDark ? resolveColor('red.300') : resolveColor('red.200'),
            text: isDark ? resolveColor('red.200') : resolveColor('red.800'),
          }
        default:
          return {
            fill: isDark ? 'rgba(237, 242, 247, 0.16)' : resolveColor('gray.100'),
            stroke: isDark ? resolveColor('gray.300') : resolveColor('gray.200'),
            text: isDark ? resolveColor('gray.200') : resolveColor('gray.800'),
          }
      }
    }

    const initialColors = getBadgeColors('blue')
    const intermediateColors = getBadgeColors('gray')
    const successColors = getBadgeColors('green')
    const failedColors = getBadgeColors('red')

    const allStates: Record<string, FsmState> = props.states.reduce(
      (accum, state) => ({ ...accum, [state.name]: state }),
      {}
    )

    // Helper to get CSS class for state type
    const getStateClass = (stateType: FsmState.Type): string => {
      switch (stateType) {
        case 'INITIAL':
          return 'initial'
        case 'SUCCESS':
          return 'success'
        case 'FAILED':
          return 'failed'
        case 'INTERMEDIATE':
        default:
          return 'intermediate'
      }
    }

    // Helper to format transition label
    const getTransitionLabel = (transition: FsmTransition): string => {
      const guards = (transition as FsmTransition & { guards?: string }).guards
      return guards ? `${transition.event}<br/>+ ${guards}` : transition.event
    }

    // Helper to check if transition is selected
    const isSelected = (transition: FsmTransition): boolean => {
      if (!props.selectedTransition) return false
      return (
        transition.event === props.selectedTransition.event &&
        transition.fromState === props.selectedTransition.from &&
        transition.toState === props.selectedTransition.to
      )
    }

    const initTransitions = props.transitions.filter((transition) => allStates[transition.fromState].type === 'INITIAL')

    // Calculate selected transition index for path highlighting
    // Transitions are rendered in order: initTransitions, then main transitions
    const selectedTransitionIndex = props.selectedTransition
      ? initTransitions.length + props.transitions.findIndex(isSelected)
      : -1
    const terminalNodes = props.transitions.reduce<string[]>((accum, transition) => {
      if (allStates[transition.toState].type === 'FAILED' || allStates[transition.toState].type === 'SUCCESS') {
        if (!accum.includes(transition.toState)) accum.push(transition.toState)
      }
      return accum
    }, [])

    // Build Mermaid script with theme-aware colors
    const script = [
      'stateDiagram-v2',
      '',
      // Class definitions matching Chakra UI Badge styling
      `classDef initial fill:${initialColors.fill},stroke:${initialColors.stroke},color:${initialColors.text},stroke-width:2px`,
      `classDef intermediate fill:${intermediateColors.fill},stroke:${intermediateColors.stroke},color:${intermediateColors.text},stroke-width:2px`,
      `classDef success fill:${successColors.fill},stroke:${successColors.stroke},color:${successColors.text},stroke-width:2px`,
      `classDef failed fill:${failedColors.fill},stroke:${failedColors.stroke},color:${failedColors.text},stroke-width:2px`,
      `classDef selectedInitial fill:${initialColors.fill},stroke:${initialColors.stroke},color:${initialColors.text},stroke-width:12px`,
      `classDef selectedIntermediate fill:${intermediateColors.fill},stroke:${intermediateColors.stroke},color:${intermediateColors.text},stroke-width:12px`,
      `classDef selectedSuccess fill:${successColors.fill},stroke:${successColors.stroke},color:${successColors.text},stroke-width:12px`,
      `classDef selectedFailed fill:${failedColors.fill},stroke:${failedColors.stroke},color:${failedColors.text},stroke-width:12px`,
      '',
      // Initial state transitions
      ...initTransitions.map((transition) => `[*] --> ${transition.fromState}`),
      '',
      // Main transitions with labels
      ...props.transitions.map(
        (transition) => `${transition.fromState} --> ${transition.toState} : ${getTransitionLabel(transition)}`
      ),
      '',
      // Terminal state transitions
      ...terminalNodes.map((node) => `${node} --> [*]`),
      'Disconnected --> [*]',
      '',
      // Apply classes to states using 'class' keyword
      ...props.states.map((state) => {
        // Check if this state is part of the selected transition
        const isPartOfSelection =
          props.selectedTransition &&
          (state.name === props.selectedTransition.from || state.name === props.selectedTransition.to)

        if (isPartOfSelection) {
          // Apply the selected variant class (thicker stroke)
          const selectedClass = `selected${getStateClass(state.type).charAt(0).toUpperCase() + getStateClass(state.type).slice(1)}`
          return `class ${state.name} ${selectedClass}`
        }
        return `class ${state.name} ${getStateClass(state.type)}`
      }),
    ]

    const finalScript = script.join('\n')
    console.log('MermaidRenderer - Generated script:')
    console.log(finalScript)
    console.log('MermaidRenderer - selectedTransition:', props.selectedTransition)
    console.log('MermaidRenderer - selectedTransitionIndex:', selectedTransitionIndex)
    console.log('MermaidRenderer - colorMode:', colorMode)
    return { script: finalScript, index: selectedTransitionIndex }
  }, [props.states, props.transitions, props.selectedTransition, colorMode, theme])

  //
  if (!script) return null

  return <Mermaid text={script.script} selectedTransitionIndex={script.index >= 0 ? script.index : undefined} />
}
