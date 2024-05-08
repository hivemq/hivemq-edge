import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { ArrayFieldTemplateItemType, getTemplate, getUiOptions } from '@rjsf/utils'
import { Box, ButtonGroup, FormControl, HStack, useDisclosure, VStack } from '@chakra-ui/react'
import { LuPanelTopClose, LuPanelTopOpen } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'

// TODO[NVL] Need a better handling of the custom UISchema property, for the Adapter SDK
interface ArrayFieldItemCollapsableUISchema {
  titleKey: string
}

// TODO[NVL] This is driven by subscription handling; use uiSchema to allow configuration for individual array property
export const ArrayFieldItemTemplate: FC<ArrayFieldTemplateItemType> = (props) => {
  const {
    children,
    disabled,
    hasToolbar,
    hasCopy,
    hasMoveDown,
    hasMoveUp,
    hasRemove,
    index,
    onCopyIndexClick,
    onDropIndexClick,
    onReorderClick,
    readonly,
    uiSchema,
    registry,
  } = props
  const { t } = useTranslation('components')
  const uiOptions = getUiOptions(uiSchema)
  const collapsableItems: ArrayFieldItemCollapsableUISchema | undefined = useMemo(() => {
    return uiOptions.collapsable as ArrayFieldItemCollapsableUISchema | undefined
  }, [uiOptions.collapsable])
  const { isOpen, onToggle, getButtonProps, getDisclosureProps } = useDisclosure({
    defaultIsOpen:
      !collapsableItems ||
      !collapsableItems?.titleKey ||
      (index === props.totalItems - 1 && children.props.formData[collapsableItems?.titleKey] === undefined),
  })
  const name = useMemo<string>(() => {
    const childrenFormData = collapsableItems?.titleKey
      ? children.props.formData[collapsableItems?.titleKey]
      : undefined
    if (childrenFormData) return `${children.props.name} - ${childrenFormData}`
    return children.props.name
  }, [children.props.formData, children.props.name, collapsableItems?.titleKey])

  const { CopyButton, MoveDownButton, MoveUpButton, RemoveButton } = registry.templates.ButtonTemplates
  const onCopyClick = useMemo(() => onCopyIndexClick(index), [index, onCopyIndexClick])
  const onRemoveClick = useMemo(() => onDropIndexClick(index), [index, onDropIndexClick])
  const onArrowUpClick = useMemo(() => onReorderClick(index, index - 1), [index, onReorderClick])
  const onArrowDownClick = useMemo(() => onReorderClick(index, index + 1), [index, onReorderClick])

  const renderCollapsed = () => {
    const TitleFieldTemplate = getTemplate<'TitleFieldTemplate'>('TitleFieldTemplate', registry, uiOptions)
    return (
      <FormControl variant="hivemq">
        <TitleFieldTemplate
          title={name}
          id={children.props.name}
          registry={registry}
          uiSchema={uiSchema}
          schema={props.schema}
        />
      </FormControl>
    )
  }
  const { hidden, ...rest } = getDisclosureProps()

  return (
    <HStack flexDirection="row-reverse" alignItems="flex-start" py={1} role="listitem">
      {hasToolbar && (
        <VStack gap={6} role="toolbar">
          {collapsableItems && (
            <ButtonGroup>
              <IconButton
                icon={isOpen ? <LuPanelTopClose /> : <LuPanelTopOpen />}
                onClick={onToggle}
                {...getButtonProps()}
                aria-label={t('rjsf.ArrayFieldItem.Buttons.expanded', { context: isOpen ? 'true' : 'false' })}
              />
            </ButtonGroup>
          )}
          {isOpen && (
            <ButtonGroup isAttached mb={1} orientation="vertical">
              {(hasMoveUp || hasMoveDown) && (
                <MoveUpButton
                  disabled={disabled || readonly || !hasMoveUp}
                  onClick={onArrowUpClick}
                  uiSchema={uiSchema}
                  registry={registry}
                />
              )}
              {(hasMoveUp || hasMoveDown) && (
                <MoveDownButton
                  disabled={disabled || readonly || !hasMoveDown}
                  onClick={onArrowDownClick}
                  uiSchema={uiSchema}
                  registry={registry}
                />
              )}
              {hasCopy && (
                <CopyButton
                  disabled={disabled || readonly}
                  onClick={onCopyClick}
                  uiSchema={uiSchema}
                  registry={registry}
                />
              )}
              {hasRemove && (
                <RemoveButton
                  disabled={disabled || readonly}
                  onClick={onRemoveClick}
                  uiSchema={uiSchema}
                  registry={registry}
                />
              )}
            </ButtonGroup>
          )}
        </VStack>
      )}

      <Box w="100%" {...rest}>
        {!collapsableItems || isOpen ? children : renderCollapsed()}
      </Box>
    </HStack>
  )
}
