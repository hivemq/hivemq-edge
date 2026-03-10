import type { FC } from 'react'
import { useState, useMemo } from 'react'
import { v4 as uuidv4 } from 'uuid'
import { useTranslation } from 'react-i18next'
import type { ColumnDef } from '@tanstack/react-table'
import type { SingleValue } from 'chakra-react-select'
import { ButtonGroup, HStack, Text } from '@chakra-ui/react'
import type { FieldProps, RJSFSchema } from '@rjsf/utils'
import { LuPencil, LuPlus, LuTrash } from 'react-icons/lu'

import type { DataCombining, ManagedAsset } from '@/api/__generated__'
import { EntityType, DataIdentifierReference } from '@/api/__generated__'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable'
import IconButton from '@/components/Chakra/IconButton'
import { PLCTag, Topic, TopicFilter } from '@/components/MQTT/EntityTag'
import { formatOwnershipString } from '@/components/MQTT/topic-utils'

import { PrimaryWrapper } from '@/modules/Mappings/combiner/components/PrimaryWrapper.tsx'
import DataCombiningEditorDrawer from '@/modules/Mappings/combiner/DataCombiningEditorDrawer.tsx'
import type { CombinerContext } from '@/modules/Mappings/types.ts'
import ManagedAssetSelect from '@/modules/Pulse/components/assets/ManagedAssetSelect.tsx'
import AssetNameCell from '@/modules/Pulse/components/assets/AssetNameCell.tsx'

export const DataCombiningTableField: FC<FieldProps<DataCombining[], RJSFSchema, CombinerContext>> = (props) => {
  const { t } = useTranslation()
  const [selectedItem, setSelectedItem] = useState<number | undefined>(undefined)

  const isAssetManager = useMemo(() => {
    return props.formContext?.entities?.some((e) => e.type === EntityType.PULSE_AGENT)
  }, [props.formContext?.entities])

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

  const handleAddAsset = (asset: SingleValue<ManagedAsset>) => {
    if (!asset) return
    const newMapping: DataCombining = {
      id: uuidv4(),
      sources: {
        primary: { id: '', type: DataIdentifierReference.type.TAG },
      },
      destination: { topic: asset.topic, assetId: asset.id, schema: asset.schema },
      instructions: [],
    }
    props.onChange([...(props.formData || []), newMapping])
  }

  const displayColumns = useMemo<ColumnDef<DataCombining>[]>(() => {
    const handleAdd = () => {
      const newMapping: DataCombining = {
        id: uuidv4(),
        sources: {
          primary: { id: '', type: DataIdentifierReference.type.TAG },
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

    const assetColumn: ColumnDef<DataCombining> = {
      accessorKey: 'destination.assetId',
      cell: (info) => {
        return <AssetNameCell assetId={info.row.original.destination.assetId} />
      },
      header: t('pulse.assets.listing.column.name'),
    }

    return [
      ...(isAssetManager ? [assetColumn] : []),
      {
        accessorKey: 'destination.topic',
        cell: (info) => {
          if (info.row.original.destination.topic) return <Topic tagTitle={info.row.original.destination.topic} />
          return <Text>{t('combiner.unset')}</Text>
        },
        header: t('pulse.assets.listing.column.topic'),
      },
      {
        accessorKey: 'sources',
        header: t('pulse.assets.listing.column.sources'),
        cell: (info) => {
          const row = info.row.original
          const primary = row.sources.primary

          const uniqueRefs = (row.instructions ?? [])
            .filter((inst) => inst.sourceRef != null)
            .map((inst) => inst.sourceRef!)
            .reduce<DataIdentifierReference[]>((acc, ref) => {
              const exists = acc.find((r) => r.id === ref.id && r.type === ref.type && r.scope === ref.scope)
              return exists ? acc : [...acc, ref]
            }, [])

          if (uniqueRefs.length === 0) return <Text>{t('combiner.unset')}</Text>

          const isPrimary = (ref: DataIdentifierReference): boolean =>
            primary?.type === ref.type &&
            primary?.id === ref.id &&
            (ref.type !== DataIdentifierReference.type.TAG || primary?.scope === ref.scope)

          return (
            <HStack flexWrap="wrap">
              {uniqueRefs
                .filter((ref) => ref.type === DataIdentifierReference.type.TAG)
                .map((ref) => (
                  <PrimaryWrapper key={`${ref.id}@${ref.scope ?? ''}`} isPrimary={isPrimary(ref)}>
                    <PLCTag tagTitle={formatOwnershipString(ref)} />
                  </PrimaryWrapper>
                ))}
              {uniqueRefs
                .filter((ref) => ref.type === DataIdentifierReference.type.TOPIC_FILTER)
                .map((ref) => (
                  <PrimaryWrapper key={ref.id} isPrimary={isPrimary(ref)}>
                    <TopicFilter tagTitle={ref.id} />
                  </PrimaryWrapper>
                ))}
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
                data-testid="combiner-mapping-list-edit"
                icon={<LuPencil />}
                onClick={() => setSelectedItem(info.row.index)}
              />
              <IconButton
                aria-label={t('combiner.schema.mappings.table.delete')}
                data-testid="combiner-mapping-list-delete"
                icon={<LuTrash />}
                onClick={() => handleDelete(info.row.index)}
              />
            </ButtonGroup>
          )
        },
        footer: () => {
          if (isAssetManager) return null
          return (
            <ButtonGroup isAttached size="sm">
              <IconButton
                data-testid="combiner-mapping-list-add"
                aria-label={t('combiner.schema.mappings.table.add')}
                icon={<LuPlus />}
                onClick={handleAdd}
              />
            </ButtonGroup>
          )
        },
      },
    ]
  }, [isAssetManager, props, t])

  return (
    <>
      <PaginatedTable<DataCombining>
        aria-label={t('combiner.schema.mappings.description')}
        data={props.formData || []}
        columns={displayColumns}
        enablePaginationGoTo={false}
        enablePaginationSizes={false}
        enableColumnFilters={false}
      />
      {isAssetManager && <ManagedAssetSelect mappings={props.formData || []} onChange={handleAddAsset} />}

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
