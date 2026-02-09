import debug from 'debug'
import type { FC } from 'react'
import { useMemo } from 'react'
import { useTheme, useColorMode } from '@chakra-ui/react'

import { Mermaid } from '@/components/Mermaid.tsx'

import type { FiniteStateMachine, FsmState, FsmTransition } from '@datahub/types.ts'
import { getBadgeColors, getStateClass, getTransitionLabel } from './MermaidRenderer.utils'

const datahubLog = debug('DataHub:Mermaid')

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
    if (!props.states || !props.transitions) {
      datahubLog('MermaidRenderer - missing states or transitions')
      return null
    }

    const initialColors = getBadgeColors('blue', theme, colorMode)
    const intermediateColors = getBadgeColors('gray', theme, colorMode)
    const successColors = getBadgeColors('green', theme, colorMode)
    const failedColors = getBadgeColors('red', theme, colorMode)

    const allStates: Record<string, FsmState> = props.states.reduce(
      (accum, state) => ({ ...accum, [state.name]: state }),
      {}
    )

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
    return { script: finalScript, index: selectedTransitionIndex }
  }, [props.states, props.transitions, props.selectedTransition, colorMode, theme])

  //
  if (!script) return null

  return <Mermaid text={script.script} selectedTransitionIndex={script.index >= 0 ? script.index : undefined} />
}
