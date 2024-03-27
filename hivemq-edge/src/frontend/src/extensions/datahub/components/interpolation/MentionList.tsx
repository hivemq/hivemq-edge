import { forwardRef, useEffect, useImperativeHandle, useState } from 'react'
import { Button, Card } from '@chakra-ui/react'
import { SuggestionOptions, SuggestionProps } from '@tiptap/suggestion'

// This type is based on
// https://github.com/ueberdosis/tiptap/blob/a27c35ac8f1afc9d51f235271814702bc72f1e01/packages/extension-mention/src/mention.ts#L73-L103.
// TODO(Steven DeMartini): Use the Tiptap exported MentionNodeAttrs interface
// once https://github.com/ueberdosis/tiptap/pull/4136 is merged.
export interface MentionNodeAttrs {
  id: string
  label: string
}

export type SuggestionListRef = {
  // For convenience using this SuggestionList from within the
  // mentionSuggestionOptions, we'll match the signature of SuggestionOptions's
  // `onKeyDown` returned in its `render` function
  onKeyDown: NonNullable<ReturnType<NonNullable<SuggestionOptions<MentionNodeAttrs>['render']>>['onKeyDown']>
}

export type SuggestionListProps = SuggestionProps<MentionNodeAttrs>

const SuggestionList = forwardRef<SuggestionListRef, SuggestionListProps>((props, ref) => {
  const [selectedIndex, setSelectedIndex] = useState(0)

  const selectItem = (index: number) => {
    if (index >= props.items.length) {
      return
    }

    const suggestion = props.items[index]
    props.command(suggestion)
  }

  const upHandler = () => {
    setSelectedIndex((selectedIndex + props.items.length - 1) % props.items.length)
  }

  const downHandler = () => {
    setSelectedIndex((selectedIndex + 1) % props.items.length)
  }

  const enterHandler = () => {
    selectItem(selectedIndex)
  }

  useEffect(() => setSelectedIndex(0), [props.items])

  useImperativeHandle(ref, () => ({
    onKeyDown: ({ event }) => {
      if (event.key === 'ArrowUp') {
        upHandler()
        return true
      }

      if (event.key === 'ArrowDown') {
        downHandler()
        return true
      }

      if (event.key === 'Enter') {
        enterHandler()
        return true
      }

      return false
    },
  }))

  return (
    <Card className="items">
      {props.items.length ? (
        props.items.map((item, index) => (
          <Button
            className={`item ${index === selectedIndex ? 'is-selected' : ''}`}
            key={item.id}
            onClick={() => selectItem(index)}
          >
            {item.label}
          </Button>
        ))
      ) : (
        <div className="item">No result</div>
      )}
    </Card>
  )
})

SuggestionList.displayName = 'SuggestionList'

export default SuggestionList
