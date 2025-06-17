import { forwardRef, useEffect, useImperativeHandle, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertDescription, AlertTitle, Button, Spinner, VisuallyHidden, VStack } from '@chakra-ui/react'
import type { SuggestionOptions, SuggestionProps } from '@tiptap/suggestion'
import type { MentionNodeAttrs } from '@tiptap/extension-mention'

import type { InterpolationVariable } from '@/api/__generated__'
import { PolicyType } from '@/api/__generated__'
import { useGetInterpolationVariables } from '@datahub/api/hooks/DataHubInterpolationService/useGetInterpolationVariables.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { DataHubNodeType } from '@datahub/types.ts'

export type SuggestionListRef = {
  // For convenience using this SuggestionList from within the
  // mentionSuggestionOptions, we'll match the signature of SuggestionOptions's
  // `onKeyDown` returned in its `render` function
  onKeyDown: NonNullable<ReturnType<NonNullable<SuggestionOptions<MentionNodeAttrs>['render']>>['onKeyDown']>
}

export type SuggestionListProps = SuggestionProps<MentionNodeAttrs>

const SuggestionList = forwardRef<SuggestionListRef, SuggestionListProps>(({ query, command }, ref) => {
  const { t } = useTranslation('datahub')
  const [selectedIndex, setSelectedIndex] = useState(0)
  const { data, error, isLoading, isError } = useGetInterpolationVariables()
  const { type } = useDataHubDraftStore()

  /**
   * Bypassing the logic implemented in the `getItems` utility function, to use the request-based list
   */
  const filterItems = useMemo<InterpolationVariable[] | undefined>(() => {
    if (!data || !type || isError) {
      return undefined
    }

    return data.items.filter(
      (item) =>
        ((item.policyType.includes(PolicyType.DATA_POLICY) && type === DataHubNodeType.DATA_POLICY) ||
          (item.policyType.includes(PolicyType.BEHAVIOR_POLICY) && type === DataHubNodeType.BEHAVIOR_POLICY)) &&
        (query === '' || item.variable.toLowerCase().includes(query.toLowerCase()))
    )
  }, [data, isError, query, type])

  const selectItem = (index: number) => {
    if (!filterItems || index >= filterItems.length) {
      return
    }

    const suggestion = filterItems[index]
    command({ id: suggestion.variable, label: suggestion.variable } as MentionNodeAttrs)
  }

  useEffect(() => setSelectedIndex(0), [filterItems])

  useImperativeHandle(ref, () => ({
    onKeyDown: ({ event }) => {
      if (!filterItems) {
        return false
      }

      if (event.key === 'ArrowUp') {
        setSelectedIndex((selectedIndex + filterItems.length - 1) % filterItems.length)
        return true
      }

      if (event.key === 'ArrowDown') {
        setSelectedIndex((selectedIndex + 1) % filterItems.length)
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
      role="listbox"
      aria-label={t('workspace.interpolation.suggestions')}
      tabIndex={0}
    >
      {isLoading && (
        <Alert status="loading" size="sm" gap={2}>
          <Spinner size="sm" data-testid="suggestion-loading-spinner" />
          <AlertTitle> {t('workspace.interpolation.loading')}</AlertTitle>
        </Alert>
      )}
      {!isLoading && !filterItems && (
        <Alert status="error" size="sm" flexDirection="column" alignItems="flex-start">
          <AlertTitle> {t('workspace.interpolation.errorLoading')}</AlertTitle>
          {error && <AlertDescription>{error.message}</AlertDescription>}
        </Alert>
      )}
      {filterItems?.length === 0 && (
        <Alert status="warning" size="sm">
          <AlertTitle> {t('workspace.interpolation.noResult')}</AlertTitle>
        </Alert>
      )}
      {filterItems?.map((item, index) => (
        <Button
          size="sm"
          colorScheme="gray"
          variant={index === selectedIndex ? 'solid' : 'ghost'}
          key={item.variable}
          onClick={() => selectItem(index)}
          role="option"
          aria-selected={index === selectedIndex}
          aria-describedby={`${item.variable}-description`}
          aria-label={item.variable}
        >
          <VisuallyHidden data-testid="suggestion-description" id={`${item.variable}-description`}>
            {item.description}
          </VisuallyHidden>
          {item.variable}
        </Button>
      ))}
    </VStack>
  )
})

SuggestionList.displayName = 'SuggestionList'

export default SuggestionList
