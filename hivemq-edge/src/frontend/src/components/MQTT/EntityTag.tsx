import type { FC, ReactNode } from 'react'
import type { TagProps } from '@chakra-ui/react'
import { Tag, TagLabel } from '@chakra-ui/react'

import { formatTopicString } from '@/components/MQTT/topic-utils.ts'
import { ClientIcon, PLCTagIcon, TopicFilterIcon, TopicIcon } from '@/components/Icons/TopicIcon.tsx'

// TODO[NVL] Not sure adding ReactNode as possible children is a good move.
interface CustomTagProps extends TagProps {
  tagTitle: ReactNode
}

interface EntityTagProps extends CustomTagProps {
  tagIcon: React.ElementType
  colorScheme?: string
}

export const EntityTag: FC<EntityTagProps> = ({ tagTitle, tagIcon: TagIcon, colorScheme, ...rest }) => {
  const expandedTagTitle = typeof tagTitle === 'string' ? formatTopicString(tagTitle) : tagTitle
  return (
    <Tag data-testid="topic-wrapper" {...rest} letterSpacing="-0.05rem" colorScheme={colorScheme} role={'group'}>
      <TagIcon boxSize="12px" mr={2} />
      {typeof tagTitle === 'string' ? <TagLabel>{expandedTagTitle}</TagLabel> : tagTitle}
    </Tag>
  )
}

export const PLCTag: FC<CustomTagProps> = ({ tagTitle, ...rest }) => (
  <EntityTag tagIcon={PLCTagIcon} tagTitle={tagTitle} {...rest} colorScheme="blue" />
)

export const ClientTag: FC<CustomTagProps> = ({ tagTitle, ...rest }) => (
  <EntityTag tagIcon={ClientIcon} tagTitle={tagTitle} {...rest} />
)

export const Topic: FC<CustomTagProps> = ({ tagTitle, ...rest }) => (
  <EntityTag tagIcon={TopicIcon} tagTitle={tagTitle} {...rest} colorScheme="gray" />
)

export const TopicFilter: FC<CustomTagProps> = ({ tagTitle, ...rest }) => (
  <EntityTag tagIcon={TopicFilterIcon} tagTitle={tagTitle} {...rest} colorScheme="cyan" />
)
