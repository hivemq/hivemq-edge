import type { ChangeEvent, FC } from 'react'
import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Divider, Flex, FormControl, FormLabel, Input, Select, Spinner, Text } from '@chakra-ui/react'

import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'
import type { BrowseFormat } from '@/api/hooks/useProtocolAdapters/useBrowseDeviceTags.ts'
import { useBrowseDeviceTags } from '@/api/hooks/useProtocolAdapters/useBrowseDeviceTags.ts'
import type { ImportMode } from '@/api/hooks/useProtocolAdapters/useImportDeviceTags.ts'
import { useImportDeviceTags } from '@/api/hooks/useProtocolAdapters/useImportDeviceTags.ts'
import { downloadTimeStamp } from '@/utils/download.utils.ts'

interface DeviceTagBrowsePanelProps {
  adapterId: string
}

const BROWSE_FORMATS: { value: BrowseFormat; label: string; ext: string }[] = [
  { value: 'text/csv', label: 'CSV', ext: 'csv' },
  { value: 'application/json', label: 'JSON', ext: 'json' },
  { value: 'application/yaml', label: 'YAML', ext: 'yaml' },
]

const IMPORT_MODES: { value: ImportMode; label: string }[] = [
  { value: 'MERGE_SAFE', label: 'Merge (safe)' },
  { value: 'MERGE_OVERWRITE', label: 'Merge (overwrite)' },
  { value: 'OVERWRITE', label: 'Overwrite' },
  { value: 'CREATE', label: 'Create only' },
  { value: 'DELETE', label: 'Delete' },
]

const DeviceTagBrowsePanel: FC<DeviceTagBrowsePanelProps> = ({ adapterId }) => {
  const { t } = useTranslation()
  const { successToast, errorToast } = useEdgeToast()

  const [rootId, setRootId] = useState('')
  const [maxDepth, setMaxDepth] = useState('')
  const [browseFormat, setBrowseFormat] = useState<BrowseFormat>('text/csv')
  const [importMode, setImportMode] = useState<ImportMode>('MERGE_SAFE')
  const [importFile, setImportFile] = useState<File | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const { mutate: browse, isPending: isBrowsing } = useBrowseDeviceTags()
  const { mutate: importTags, isPending: isImporting } = useImportDeviceTags()

  const handleBrowse = () => {
    const ext = BROWSE_FORMATS.find((f) => f.value === browseFormat)?.ext ?? 'csv'
    browse(
      {
        adapterId,
        format: browseFormat,
        rootId: rootId || undefined,
        maxDepth: maxDepth !== '' ? Number(maxDepth) : undefined,
      },
      {
        onSuccess: (blob) => {
          const url = URL.createObjectURL(blob)
          const a = document.createElement('a')
          a.href = url
          a.download = `tags-${adapterId}-${downloadTimeStamp()}.${ext}`
          a.click()
          URL.revokeObjectURL(url)
        },
        onError: (err) => {
          errorToast({ title: t('deviceTagBrowse.error.browse') }, err)
        },
      }
    )
  }

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    setImportFile(e.target.files?.[0] ?? null)
  }

  const handleImport = () => {
    if (!importFile) return
    const format = importFile.name.endsWith('.json')
      ? 'application/json'
      : importFile.name.endsWith('.yaml') || importFile.name.endsWith('.yml')
        ? 'application/yaml'
        : 'text/csv'
    importTags(
      { adapterId, file: importFile, format, mode: importMode },
      {
        onSuccess: () => {
          successToast({ title: t('deviceTagBrowse.success.import') })
          setImportFile(null)
          if (fileInputRef.current) fileInputRef.current.value = ''
        },
        onError: (err) => {
          errorToast({ title: t('deviceTagBrowse.error.import') }, err)
        },
      }
    )
  }

  return (
    <Flex direction="column" gap={4}>
      <Text fontWeight="semibold">{t('deviceTagBrowse.section.browse')}</Text>

      <Flex gap={3} wrap="wrap">
        <FormControl flex="1" minW="160px">
          <FormLabel fontSize="sm">{t('deviceTagBrowse.field.rootId')}</FormLabel>
          <Input
            size="sm"
            placeholder={t('deviceTagBrowse.field.rootId.placeholder')}
            value={rootId}
            onChange={(e) => setRootId(e.target.value)}
          />
        </FormControl>

        <FormControl w="110px">
          <FormLabel fontSize="sm">{t('deviceTagBrowse.field.maxDepth')}</FormLabel>
          <Input
            size="sm"
            type="number"
            min={0}
            placeholder="0"
            value={maxDepth}
            onChange={(e) => setMaxDepth(e.target.value)}
          />
        </FormControl>

        <FormControl w="110px">
          <FormLabel fontSize="sm">{t('deviceTagBrowse.field.format')}</FormLabel>
          <Select size="sm" value={browseFormat} onChange={(e) => setBrowseFormat(e.target.value as BrowseFormat)}>
            {BROWSE_FORMATS.map((f) => (
              <option key={f.value} value={f.value}>
                {f.label}
              </option>
            ))}
          </Select>
        </FormControl>
      </Flex>

      <Button variant="primary" size="sm" onClick={handleBrowse} isDisabled={isBrowsing} alignSelf="flex-start">
        {isBrowsing ? <Spinner size="xs" mr={2} /> : null}
        {t('deviceTagBrowse.action.browse')}
      </Button>

      <Divider />

      <Text fontWeight="semibold">{t('deviceTagBrowse.section.import')}</Text>

      <Flex gap={3} wrap="wrap" align="flex-end">
        <FormControl flex="1" minW="200px">
          <FormLabel fontSize="sm">{t('deviceTagBrowse.field.file')}</FormLabel>
          <Input
            ref={fileInputRef}
            size="sm"
            type="file"
            accept=".csv,.json,.yaml,.yml"
            onChange={handleFileChange}
            sx={{ pt: '4px' }}
          />
        </FormControl>

        <FormControl w="160px">
          <FormLabel fontSize="sm">{t('deviceTagBrowse.field.importMode')}</FormLabel>
          <Select size="sm" value={importMode} onChange={(e) => setImportMode(e.target.value as ImportMode)}>
            {IMPORT_MODES.map((m) => (
              <option key={m.value} value={m.value}>
                {m.label}
              </option>
            ))}
          </Select>
        </FormControl>
      </Flex>

      <Button
        variant="primary"
        size="sm"
        onClick={handleImport}
        isDisabled={!importFile || isImporting}
        alignSelf="flex-start"
      >
        {isImporting ? <Spinner size="xs" mr={2} /> : null}
        {t('deviceTagBrowse.action.import')}
      </Button>
    </Flex>
  )
}

export default DeviceTagBrowsePanel
