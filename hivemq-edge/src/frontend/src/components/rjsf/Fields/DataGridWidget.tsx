import { FC } from 'react'
import { DataGridProps, FormDataItem } from '@/components/rjsf/Fields/types.ts'
import DataGrid, { textEditor, Column } from 'react-data-grid'

import './styles.css'
import { Box } from '@chakra-ui/react'
import IconButton from '@/components/Chakra/IconButton.tsx'
import { LuPlus, LuTrash2 } from 'react-icons/lu'
import { useTranslation } from 'react-i18next'

const DataGridWidget: FC<DataGridProps> = ({
  data,
  onHandleAddItem,
  onHandleDeleteItem,
  onSetData,
  maxItems,
  columnTypes,
  isDisabled,
}) => {
  const { t } = useTranslation('components')
  const isFull = maxItems !== undefined && data.length > maxItems - 1

  const columns = columnTypes.map<Column<FormDataItem>>(([key, value]) => ({
    key: key,
    name: value.title as string,
    renderEditCell: textEditor,
  }))

  return (
    <Box>
      <DataGrid
        className="rdg-light XXXXX"
        columns={[
          // { key: 'name', name: 'ID', renderEditCell: textEditor },
          // { key: 'value', name: 'Title', renderEditCell: textEditor },
          ...columns,
          {
            key: 'action',
            name: 'Action',
            editable: false,
            renderCell: (props) => (
              <IconButton
                icon={<LuTrash2 />}
                aria-label={t('rjsf.CompactArrayField.action.delete')}
                size="sm"
                isDisabled={isDisabled}
                onClick={() => onHandleDeleteItem?.(props.row.index)}
              />
            ),
            renderSummaryCell: () => 'fddfdfdf',
          },
        ]}
        rows={data}
        onRowsChange={onSetData}
      />
      <IconButton
        icon={<LuPlus />}
        aria-label={t('rjsf.CompactArrayField.action.add')}
        size="sm"
        isDisabled={isDisabled || isFull}
        onClick={onHandleAddItem}
      />
    </Box>
  )
}

export default DataGridWidget
