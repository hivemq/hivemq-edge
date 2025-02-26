import type { FC } from 'react'
import { useState } from 'react'
import { useMemo } from 'react'
import { v4 as uuidv4 } from 'uuid'
import { useTranslation } from 'react-i18next'
import type { ColumnDef } from '@tanstack/react-table'
import { ButtonGroup, HStack, Text } from '@chakra-ui/react'
import type { FieldProps, RJSFSchema } from '@rjsf/utils'
import { LuPencil, LuPlus, LuTrash } from 'react-icons/lu'

import { DataCombining } from '@/api/__generated__'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable'
import IconButton from '@/components/Chakra/IconButton'
import { PLCTag, Topic, TopicFilter } from '@/components/MQTT/EntityTag'
import DataCombiningEditorDrawer from './DataCombiningEditorDrawer'
import type { CombinerContext } from '../../types'

export const DataCombiningTableField: FC<FieldProps<DataCombining[], RJSFSchema, CombinerContext>> = (props) => {
  const { t } = useTranslation()
  const [selectedItem, setSelectedItem] = useState<number | undefined>(undefined)

  const handleOnSubmit = (data: DataCombining | undefined) => {
    if (selectedItem === undefined) return
    if (data) {
      if (!props.formData?.[selectedItem]) throw new Error(t('combiner.error.invalidData'))
      if (data.id !== props.formData?.[selectedItem].id) throw new Error(t('combiner.error.invalidData'))

      const newValues = [...props.formData]
      newValues[selectedItem] = data
      props.onChange(newValues)
    }
    setSelectedItem(undefined)
  }

  const displayColumns = useMemo<ColumnDef<DataCombining>[]>(() => {
    const handleAdd = () => {
      const newMapping: DataCombining = {
        id: uuidv4(),
        sources: {
          primary: '',
          primaryType: DataCombining.primaryType.TAG,
          tags: [],
          topicFilters: [],
        },
        destination: { topic: '' },
        instructions: [],
      }
      props.onChange([...(props.formData || []), newMapping])
    }

    const handleDelete = (index: number) => {
      const newValues = [...(props.formData || [])]
      newValues.splice(index, 1)
      props.onChange(newValues)
    }

    return [
      // {
      //   accessorKey: 'id',
      // },
      {
        accessorKey: 'destination',
        cell: (info) => {
          if (info.row.original.destination) return <Topic tagTitle={info.row.original.destination.topic} />
          return <Text>{t('combiner.unset')}</Text>
        },
      },
      {
        accessorKey: 'sources',
        cell: (info) => {
          const { sources } = info.row.original
          const nbItems = (sources?.tags?.length || 0) + (sources?.topicFilters?.length || 0)
          if (nbItems === 0) return <Text>{t('combiner.unset')}</Text>

          return (
            <HStack flexWrap={'wrap'}>
              {info.row.original.sources?.tags?.map((tag) => <PLCTag tagTitle={tag} key={tag} />)}
              {info.row.original.sources?.topicFilters?.map((tag) => <TopicFilter tagTitle={tag} key={tag} />)}
            </HStack>
          )
        },
      },

      {
        id: 'actions',
        header: t('combiner.schema.mappings.table.action'),
        sortingFn: undefined,
        cell: (info) => {
          return (
            <ButtonGroup isAttached size="xs">
              <IconButton
                aria-label={t('combiner.schema.mappings.table.edit')}
                data-testid={'combiner-mapping-list-edit'}
                icon={<LuPencil />}
                onClick={() => setSelectedItem(info.row.index)}
              />
              <IconButton
                aria-label={t('combiner.schema.mappings.table.delete')}
                data-testid={'combiner-mapping-list-delete'}
                icon={<LuTrash />}
                onClick={() => handleDelete(info.row.index)}
              />{' '}
            </ButtonGroup>
          )
        },
        footer: () => {
          return (
            <ButtonGroup isAttached size="sm">
              <IconButton
                data-testid={'combiner-mapping-list-add'}
                aria-label={t('combiner.schema.mappings.table.add')}
                icon={<LuPlus />}
                onClick={handleAdd}
              />
            </ButtonGroup>
          )
        },
      },
    ]
  }, [props, t])

  return (
    <>
      <PaginatedTable<DataCombining>
        aria-label={t('combiner.schema.sources.description')}
        data={props.formData || []}
        columns={displayColumns}
        enablePaginationGoTo={false}
        enablePaginationSizes={false}
        enableColumnFilters={false}
      />
      {selectedItem != undefined && props.formData?.[selectedItem] && (
        <DataCombiningEditorDrawer
          onClose={() => setSelectedItem(undefined)}
          onSubmit={handleOnSubmit}
          schema={props.schema.items as RJSFSchema}
          uiSchema={props.uiSchema?.items}
          formData={props.formData?.[selectedItem]}
          formContext={props.formContext}
        />
      )}
    </>
  )
}
