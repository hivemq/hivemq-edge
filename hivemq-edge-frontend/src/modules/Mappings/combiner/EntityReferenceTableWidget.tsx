import { useMemo } from 'react'
import type { RJSFSchema, WidgetProps } from '@rjsf/utils'
import { useTranslation } from 'react-i18next'
import type { ColumnDef } from '@tanstack/react-table'
import { ButtonGroup, HStack } from '@chakra-ui/react'
import { LuTrash } from 'react-icons/lu'
import { Select } from 'chakra-react-select'

import type { EntityReference } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import IconButton from '@/components/Chakra/IconButton'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable'
import { EntityRenderer } from '@/modules/Mappings/combiner/EntityRenderer.tsx'
import type { AvailableEntity, CombinerContext } from '@/modules/Mappings/types.ts'

export const EntityReferenceTableWidget = (
  props: WidgetProps<WidgetProps<Array<EntityReference>, RJSFSchema>, RJSFSchema, CombinerContext>
) => {
  const { t } = useTranslation()
  const { schema } = props
  const allEntities: AvailableEntity[] = props.formContext?.availableEntities || []

  const selectOptions = useMemo(() => {
    const currentIds = new Set((props.value || []).map((e: EntityReference) => e.id))
    return allEntities
      .filter((e: AvailableEntity) => !currentIds.has(e.id))
      .map((e: AvailableEntity) => ({ value: e, label: e.label }))
  }, [allEntities, props.value])

  const handleAdd = (option: { value: AvailableEntity; label: string } | null) => {
    if (!option) return
    const { id, type } = option.value
    const newSources = [...(props.value || []), { id, type }]
    props.onChange(newSources)
    props.formContext?.onSourcesChange?.(newSources)
  }

  const handleDelete = (item: EntityReference) => {
    const newSources = (props.value || []).filter((e: EntityReference) => e.id !== item.id || e.type !== item.type)
    props.onChange(newSources)
    props.formContext?.onSourcesChange?.(newSources)
  }

  const displayColumns = useMemo<ColumnDef<EntityReference>[]>(() => {
    return [
      {
        accessorKey: 'type',
        header: t('combiner.schema.sources.table.source'),
        cell: (info) => {
          return <EntityRenderer reference={info.row.original} />
        },
      },
      {
        id: 'actions',
        header: t('combiner.schema.sources.table.action'),
        sortingFn: undefined,
        cell: (info) => {
          const isPermanent = info.row.original.type === EntityType.PULSE_AGENT
          return (
            <ButtonGroup isAttached size="sm">
              {!isPermanent && (
                <IconButton
                  aria-label={t('combiner.schema.sources.table.delete')}
                  icon={<LuTrash />}
                  onClick={() => handleDelete(info.row.original)}
                />
              )}
            </ButtonGroup>
          )
        },
      },
    ]
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [t, props.value])

  if (schema.type !== 'array') throw new Error('[RJSF] Cannot apply the template to the schema')
  return (
    <>
      <PaginatedTable<EntityReference>
        aria-label={t('combiner.schema.sources.description')}
        data={props.value}
        columns={displayColumns}
        enablePaginationGoTo={false}
        enablePaginationSizes={false}
        enableColumnFilters={false}
      />
      {selectOptions.length > 0 && (
        <HStack mt={2}>
          <Select
            placeholder={t('combiner.schema.sources.addSource', 'Add a source...')}
            options={selectOptions}
            onChange={handleAdd}
            value={null}
            size="sm"
            chakraStyles={{ container: (p) => ({ ...p, flex: 1 }) }}
          />
        </HStack>
      )}
    </>
  )
}
