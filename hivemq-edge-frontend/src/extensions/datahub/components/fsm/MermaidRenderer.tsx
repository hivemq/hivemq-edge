import type { FC } from 'react'
import { useMemo } from 'react'

import { Mermaid } from '@/components/Mermaid.tsx'

import type { FiniteStateMachine, FsmState } from '@datahub/types.ts'

export const MermaidRenderer: FC<FiniteStateMachine> = (props) => {
  const script = useMemo(() => {
    if (!props.states || !props.transitions) return null

    const allStates: Record<string, FsmState> = props.states.reduce(
      (accum, state) => ({ ...accum, [state.name]: state }),
      {}
    )

    const initTransitions = props.transitions.filter((transition) => allStates[transition.fromState].type === 'INITIAL')
    const terminalNodes = props.transitions.reduce<string[]>((accum, transition) => {
      if (allStates[transition.toState].type === 'FAILED' || allStates[transition.toState].type === 'SUCCESS') {
        if (!accum.includes(transition.toState)) accum.push(transition.toState)
      }
      return accum
    }, [])

    return ''.concat(
      'stateDiagram-v2\n',
      initTransitions.map((transition) => `[*] --> ${transition.fromState}`).join('\n'),
      '\n',
      props.transitions
        .map(
          (e) =>
            `${e.fromState} --> ${e.toState} : ${e.event}
            `
        )
        .join('\n'),
      '\n',
      terminalNodes.map((e) => `${e} --> [*]`).join('\n'),
      '\nDisconnected --> [*]'
    )
  }, [props.states, props.transitions])

  //
  if (!script) return null

  return <Mermaid text={script} />
}
