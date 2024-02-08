import { FC, useMemo } from 'react'

import { Mermaid } from '@/components/Mermaid.tsx'

import { FiniteStateMachine, FsmState } from '@datahub/types.ts'

export const MermaidRenderer: FC<FiniteStateMachine> = (props) => {
  const script = useMemo(() => {
    if (!props.states || !props.transitions) return null

    const allStates: Record<string, FsmState> = props.states.reduce(
      (accum, state) => ({ ...accum, [state.name]: state }),
      {}
    )

    const initTransitions = props.transitions.filter((e) => allStates[e.fromState].type === 'INITIAL')
    const terminalNodes = props.transitions.filter(
      (e) => allStates[e.toState].type === 'FAILED' || allStates[e.toState].type === 'SUCCESS'
    )

    return `stateDiagram-v2
    ${initTransitions.map((e) => `[*] --> ${e.fromState}`).join('\n')}
    ${props.transitions
      .map(
        (e) =>
          `${e.fromState} --> ${e.toState} : ${e.event}
            `
      )
      .join('\n')}
    ${terminalNodes.map((e) => `${e.toState} --> [*]`).join('\n')}
    Disconnected --> [*]`
  }, [props.states, props.transitions])

  if (!script) return null

  return <Mermaid text={script} />
}
