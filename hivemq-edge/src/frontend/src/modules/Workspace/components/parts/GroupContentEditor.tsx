import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { ColumnDef } from '@tanstack/react-table'
import { ButtonGroup, Card, CardBody, CardFooter, CardHeader, Icon } from '@chakra-ui/react'
import { MdPlayArrow, MdRestartAlt, MdStop } from 'react-icons/md'

import IconButton from '@/components/Chakra/IconButton.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { Group } from '@/modules/Workspace/types.ts'
import { AdapterStatusContainer } from '@/modules/ProtocolAdapters/components/adapters/AdapterStatusContainer.tsx'
import { ImUngroup } from 'react-icons/im'

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
        header: t('workspace.grouping.editor.content.header.type'),
        cell: (info) => {
          return t('workspace.device.type', { context: info.getValue<string>() })
        },
      },
      {
        accessorKey: 'data.status',
        header: t('workspace.grouping.editor.content.header.status'),
        cell: (info) => {
          return <AdapterStatusContainer id={info.row.original.data.id} />
        },
      },
      {
        accessorKey: 'data.id',
        header: t('workspace.grouping.editor.content.header.id'),
      },
      {
        id: 'actions',
        header: t('workspace.grouping.editor.content.header.actions'),
        sortingFn: undefined,
        cell: () => {
          return (
            <ButtonGroup isAttached size="xs" isDisabled>
              <IconButton
                aria-label={t('workspace.grouping.editor.content.actions.ungroup')}
                icon={<Icon as={ImUngroup} />}
                onClick={() => undefined}
              />
            </ButtonGroup>
          )
        },
        footer: () => {
          return (
            <ButtonGroup isAttached size="xs" isDisabled>
              <IconButton
                aria-label={t('workspace.grouping.editor.content.actions.startAll')}
                icon={<MdPlayArrow />}
                onClick={() => undefined}
              />
              <IconButton
                aria-label={t('workspace.grouping.editor.content.actions.stopAll')}
                icon={<MdStop />}
                onClick={() => undefined}
              />
              <IconButton
                aria-label={t('workspace.grouping.editor.content.actions.restartAll')}
                icon={<MdRestartAlt />}
                onClick={() => undefined}
              />
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
      <CardHeader data-testid="group-content-header">{t('Content Management')}</CardHeader>
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
