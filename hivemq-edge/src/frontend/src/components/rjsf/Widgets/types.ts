import type { ObjectNode } from '@/api/__generated__'

export type FlatObjectNode = Omit<ObjectNode, 'children'>
