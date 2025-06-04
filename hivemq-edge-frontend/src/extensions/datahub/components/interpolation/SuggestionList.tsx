import { forwardRef, useEffect, useImperativeHandle, useState } from 'react'
import { Alert, AlertTitle, Button, VStack } from '@chakra-ui/react'
import type { SuggestionOptions, SuggestionProps } from '@tiptap/suggestion'
import { useTranslation } from 'react-i18next'

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
  const { t } = useTranslation('datahub')
  const [selectedIndex, setSelectedIndex] = useState(0)

  const selectItem = (index: number) => {
    if (index >= props.items.length) {
      return
    }

    const suggestion = props.items[index]
    props.command(suggestion)
  }

  useEffect(() => setSelectedIndex(0), [props.items])

  useImperativeHandle(ref, () => ({
    onKeyDown: ({ event }) => {
      if (event.key === 'ArrowUp') {
        setSelectedIndex((selectedIndex + props.items.length - 1) % props.items.length)
        return true
      }

      if (event.key === 'ArrowDown') {
        setSelectedIndex((selectedIndex + 1) % props.items.length)
        return true
      }

      if (event.key === 'Enter') {
        selectItem(selectedIndex)
        return true
      }

      return false
    },
  }))

  return (
    <VStack
      // TODO[NV} Second time styling is replicated; crate a component
      data-testid="interpolation-container"
      alignItems="stretch"
      p={0}
      gap={0}
      borderWidth={1}
      bg="var(--chakra-colors-chakra-body-bg)"
      borderRadius="var(--chakra-radii-base)"
      boxShadow="var(--chakra-shadows-lg)"
    >
      {props.items.length ? (
        props.items.map((item, index) => (
          <Button
            size="sm"
            colorScheme="gray"
            variant={index === selectedIndex ? 'solid' : 'ghost'}
            key={item.id}
            onClick={() => selectItem(index)}
          >
            {item.label}
          </Button>
        ))
      ) : (
        <Alert status="warning" size="sm">
          <AlertTitle> {t('workspace.interpolation.noResult')}</AlertTitle>
        </Alert>
      )}
    </VStack>
  )
})

SuggestionList.displayName = 'SuggestionList'

export default SuggestionList
