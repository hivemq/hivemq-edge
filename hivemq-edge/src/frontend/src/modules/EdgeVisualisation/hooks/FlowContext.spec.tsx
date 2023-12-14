import { EdgeFlowOptions } from '@/modules/EdgeVisualisation/types.ts'
import { act, render } from '@testing-library/react'
import { useContext } from 'react'
import { describe, expect, it } from 'vitest'
import { EdgeFlowContext, EdgeFlowProvider } from './FlowContext.tsx'

const optionKeys = ['showTopics', 'showStatus', 'showMetrics', 'showGateway', 'showHosts']

const ProviderTestingMock = () => {
  const context = useContext(EdgeFlowContext)
  return (
    <>
      {optionKeys.map((option) => {
        const keyOption = option as keyof EdgeFlowOptions

        return (
          <input
            key={option}
            data-testid={option}
            checked={context?.options[keyOption]}
            type="checkbox"
            onChange={() => context?.setOptions((old) => ({ ...old, [option]: !context?.options[keyOption] }))}
          />
        )
      })}
    </>
  )
}

describe('EdgeFlowProvider', () => {
  it.each(optionKeys)('should change the option for %s', (option) => {
    const { getByTestId } = render(
      <EdgeFlowProvider defaults={{ [option]: true }}>
        <ProviderTestingMock />
      </EdgeFlowProvider>,
    )

    const checkOption = getByTestId(option) as HTMLInputElement
    expect(checkOption.checked).toBe(true)

    act(() => {
      /* fire events that update state */
      getByTestId(option).click()
    })
    expect(checkOption.checked).toBe(false)
    act(() => {
      /* fire events that update state */
      getByTestId(option).click()
    })
    expect(checkOption.checked).toBe(true)
  })
})
