import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { Group } from '@/modules/Workspace/types.ts'
import { ButtonGroup, Card, CardBody, CardFooter, CardHeader } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import { ColumnDef } from '@tanstack/react-table'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'
import { MdPlayArrow, MdRestartAlt, MdStop } from 'react-icons/md'
import { AdapterStatusContainer } from '@/modules/ProtocolAdapters/components/adapters/AdapterStatusContainer.tsx'

interface GroupContentEditorProps {
  group: Node<Group>
}

const GroupContentEditor: FC<GroupContentEditorProps> = ({ group }) => {
  const { t } = useTranslation()
  const { nodes } = useWorkspaceStore()

  const columns = useMemo<ColumnDef<Node>[]>(() => {
    return [
      {
        accessorKey: 'type',
      },
      {
        accessorKey: 'data.status',
        cell: (info) => {
          return <AdapterStatusContainer id={info.row.original.data.id} />
        },
      },
      {
        accessorKey: 'data.id',
      },
      {
        id: 'actions',
        header: t('topicFilter.listing.column.action'),
        sortingFn: undefined,
        footer: () => {
          return (
            <ButtonGroup isAttached size="sm" colorScheme="red">
              <IconButton aria-label={t('Stop all')} icon={<MdPlayArrow />} onClick={() => undefined} />
              <IconButton aria-label={t('Stop all')} icon={<MdStop />} onClick={() => undefined} />
              <IconButton aria-label={t('Restart all')} icon={<MdRestartAlt />} onClick={() => undefined} />
            </ButtonGroup>
          )
        },
      },
    ]
  }, [t])

  const data = useMemo<Node[]>(() => {
    return group.data.childrenNodeIds.map((e) => nodes.find((x) => x.id === e)).filter((e) => Boolean(e)) as Node[]
  }, [group.data.childrenNodeIds, nodes])

  return (
    <Card size="sm">
      <CardHeader> {t('Content Management')}</CardHeader>
      <CardBody>
        <PaginatedTable<Node>
          aria-label={t('eventLog.title')}
          data={data}
          columns={columns}
          enablePaginationSizes={false}
          enablePaginationGoTo={false}
        />
      </CardBody>
      <CardFooter justifyContent="flex-end"></CardFooter>
    </Card>
  )
}

export default GroupContentEditor
