import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { HStack, Text } from '@chakra-ui/react'

import { DataIdentifierReference } from '@/api/__generated__'
import { AssetTag, PLCTag, TopicFilter } from '@/components/MQTT/EntityTag.tsx'
import { PrimaryWrapper } from '@/modules/Mappings/combiner/components/PrimaryWrapper.tsx'
import { MAX_SOURCES_PER_ROW } from '@/modules/Pulse/utils/pagination-utils.ts'

interface SourcesCellProps {
  sources?: DataIdentifierReference[]
  primary?: DataIdentifierReference
}

const SourcesCell: FC<SourcesCellProps> = ({ sources, primary }) => {
  const { t } = useTranslation()

  const isPrimary = (type: DataIdentifierReference.type, id: string): boolean => {
    return primary?.type === type && primary?.id === id
  }

  if (!sources?.length) return <Text>{t('pulse.assets.listing.sources.unset')}</Text>
  const extraItems = Math.max(sources.length - MAX_SOURCES_PER_ROW, 0)

  return (
    <HStack flexWrap="wrap">
      {sources?.slice(0, MAX_SOURCES_PER_ROW).map((tag) => (
        <PrimaryWrapper key={tag.id} isPrimary={isPrimary(tag.type, tag.id)}>
          <>
            {tag.type === DataIdentifierReference.type.TOPIC_FILTER && <TopicFilter tagTitle={tag.id} />}
            {tag.type === DataIdentifierReference.type.TAG && <PLCTag tagTitle={tag.id} />}
            {tag.type === DataIdentifierReference.type.PULSE_ASSET && <AssetTag tagTitle={tag.id} />}
          </>
        </PrimaryWrapper>
      ))}
      {extraItems > 0 && <Text>{t('pulse.assets.listing.sources.more', { count: extraItems })}</Text>}
    </HStack>
  )
}

export default SourcesCell
