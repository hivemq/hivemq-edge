import { MOCK_SIMPLE_SCHEMA_URI } from '@/__test-utils__/rjsf/schema.mocks.ts'
import type { Combiner, CombinerList, ManagedAsset, ManagedAssetList } from '@/api/__generated__'
import { DataIdentifierReference, EntityType } from '@/api/__generated__'
import { AssetMapping } from '@/api/__generated__'

export const MOCK_PULSE_EXT_ASSET_UNMAPPED: ManagedAsset = {
  id: '3b028f58-1111-4de1-9b8b-c1a35b1643a4',
  name: 'Test asset unmapped',
  description: 'The short description of the asset',
  topic: 'test/topic',
  schema: MOCK_SIMPLE_SCHEMA_URI,
  mapping: {
    status: AssetMapping.status.UNMAPPED,
  },
}

export const MOCK_PULSE_EXT_ASSET_UNMAPPED_2: ManagedAsset = {
  id: '3b028f58-1111-4de1-9b8b-c1a35b1643a5',
  name: 'Test other asset unmapped',
  description: 'The short description of the asset',
  topic: 'test/topic',
  schema: MOCK_SIMPLE_SCHEMA_URI,
  mapping: {
    status: AssetMapping.status.UNMAPPED,
  },
}

export const MOCK_PULSE_EXT_ASSET_MAPPED_MAPPING_ID1 = 'ff02efff-7b4c-2222-1111-74d0756283fb'

export const MOCK_PULSE_EXT_ASSET_MAPPED: ManagedAsset = {
  id: '3b028f58-2222-4de1-9b8b-c1a35b1643a4',
  name: 'Test asset mapped',
  description: 'The short description of the asset',
  topic: 'test/topic/2',
  schema: MOCK_SIMPLE_SCHEMA_URI,
  mapping: {
    mappingId: MOCK_PULSE_EXT_ASSET_MAPPED_MAPPING_ID1,
    status: AssetMapping.status.STREAMING,
  },
}

export const MOCK_PULSE_EXT_ASSET_REMAPPING_ID = 'ff02efff-7b4c-2222-2222-74d0756283fb'

export const MOCK_PULSE_EXT_ASSET_REMAPPING: ManagedAsset = {
  id: '3b028f58-3333-4de1-9b8b-c1a35b1643a4',
  name: 'Test asset remapping',
  description: 'The short description of the asset',
  topic: 'test/new/topic/to/remap',
  schema: MOCK_SIMPLE_SCHEMA_URI,
  mapping: {
    mappingId: MOCK_PULSE_EXT_ASSET_REMAPPING_ID,
    status: AssetMapping.status.REQUIRES_REMAPPING,
  },
}

export const MOCK_PULSE_EXT_ASSETS_LIST: ManagedAssetList = {
  items: [
    MOCK_PULSE_EXT_ASSET_UNMAPPED,
    MOCK_PULSE_EXT_ASSET_UNMAPPED_2,
    MOCK_PULSE_EXT_ASSET_MAPPED,
    MOCK_PULSE_EXT_ASSET_REMAPPING,
  ],
}

export const MOCK_PULSE_EXT_ASSET_MAPPER: Combiner = {
  id: 'e9af7f82-aaaa-4d07-8c0f-e4591148af19',
  name: 'my first mapper',
  description: 'This is a description for the first asset mapper',
  sources: {
    items: [
      {
        type: EntityType.ADAPTER,
        id: 'my-adapter',
      },
      {
        type: EntityType.PULSE_AGENT,
        id: 'The Pulse Agent',
      },
    ],
  },
  mappings: {
    items: [
      {
        id: MOCK_PULSE_EXT_ASSET_MAPPED_MAPPING_ID1,
        sources: {
          primary: { id: '', type: DataIdentifierReference.type.TAG },
          tags: ['my/tag/t1', 'my/tag/t3'],
          topicFilters: ['my/topic/+/temp'],
        },
        destination: {
          assetId: MOCK_PULSE_EXT_ASSET_MAPPED.id,
          topic: MOCK_PULSE_EXT_ASSET_MAPPED.topic,
          schema: MOCK_PULSE_EXT_ASSET_MAPPED.schema,
        },
        instructions: [],
      },
      {
        id: MOCK_PULSE_EXT_ASSET_REMAPPING_ID,
        sources: {
          primary: { id: '', type: DataIdentifierReference.type.TAG },
          tags: ['my/tag/t1', 'my/tag/t3'],
          topicFilters: ['my/topic/+/temp'],
        },
        destination: {
          assetId: MOCK_PULSE_EXT_ASSET_REMAPPING.id,
          topic: MOCK_PULSE_EXT_ASSET_REMAPPING.topic,
          schema: MOCK_PULSE_EXT_ASSET_REMAPPING.schema,
        },
        instructions: [],
      },
    ],
  },
}

export const MOCK_PULSE_EXT_ASSET_MAPPER_EMPTY: Combiner = {
  id: 'e9af7f82-bbbb-4d07-8c0f-e4591148af19',
  name: 'my second mapper',
  description: 'This is a description for the second asset mapper',
  sources: {
    items: [
      {
        type: EntityType.ADAPTER,
        id: 'my-other-adapter',
      },
      {
        type: EntityType.PULSE_AGENT,
        id: 'The Pulse Agent',
      },
    ],
  },
  mappings: {
    items: [],
  },
}

export const MOCK_PULSE_EXT_ASSET_MAPPERS_LIST: CombinerList = {
  items: [MOCK_PULSE_EXT_ASSET_MAPPER, MOCK_PULSE_EXT_ASSET_MAPPER_EMPTY],
}
