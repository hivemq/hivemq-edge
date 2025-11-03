import { describe, expect, it } from 'vitest'
import {
  extractPolicySummary,
  extractResourcesSummary,
  extractPolicyPayload,
  groupResourcesByType,
} from './policy-summary.utils'
import {
  MOCK_SUCCESS_REPORT_DATA_POLICY,
  MOCK_SUCCESS_REPORT_BEHAVIOR_POLICY,
  MOCK_SUCCESS_REPORT_NO_RESOURCES,
  MOCK_SUCCESS_REPORT_MIXED_RESOURCES,
  MOCK_EMPTY_REPORT,
  MOCK_MALFORMED_REPORT,
} from '@datahub/__test-utils__/mock-validation-reports'
import type { DryRunResults } from '@datahub/types'
import { DataHubNodeType, DesignerStatus } from '@datahub/types'

describe('policy-summary.utils', () => {
  describe('extractPolicySummary', () => {
    it('should extract Data Policy summary for new policy', () => {
      const result = extractPolicySummary(MOCK_SUCCESS_REPORT_DATA_POLICY, DesignerStatus.DRAFT)

      expect(result).toBeDefined()
      expect(result?.id).toBe('my-data-policy')
      expect(result?.type).toBe(DataHubNodeType.DATA_POLICY)
      expect(result?.isNew).toBe(true)
      expect(result?.topicFilters).toEqual(['devices/+/temperature']) // API uses singular topicFilter
      expect(result?.transitions).toBeUndefined()
    })

    it('should extract Data Policy summary for updated policy', () => {
      const result = extractPolicySummary(MOCK_SUCCESS_REPORT_DATA_POLICY, DesignerStatus.MODIFIED)

      expect(result).toBeDefined()
      expect(result?.id).toBe('my-data-policy')
      expect(result?.isNew).toBe(false)
    })

    it('should extract Behavior Policy summary', () => {
      const result = extractPolicySummary(MOCK_SUCCESS_REPORT_BEHAVIOR_POLICY, DesignerStatus.DRAFT)

      expect(result).toBeDefined()
      expect(result?.id).toBe('my-behavior-policy')
      expect(result?.type).toBe(DataHubNodeType.BEHAVIOR_POLICY)
      expect(result?.isNew).toBe(true)
      expect(result?.topicFilters).toBeUndefined()
      expect(result?.transitions).toEqual(['Mqtt.OnInboundPublish'])
    })

    it.each([
      { name: 'empty report', report: MOCK_EMPTY_REPORT },
      { name: 'undefined report', report: undefined },
      { name: 'malformed report without data', report: MOCK_MALFORMED_REPORT },
    ])('should handle $name', ({ report }) => {
      const result = extractPolicySummary(report, DesignerStatus.DRAFT)

      expect(result).toBeUndefined()
    })

    it('should handle policy with no topic filters', () => {
      const result = extractPolicySummary(MOCK_SUCCESS_REPORT_NO_RESOURCES, DesignerStatus.DRAFT)

      expect(result).toBeDefined()
      expect(result?.topicFilters).toEqual(['test/topic'])
    })
  })

  describe('extractResourcesSummary', () => {
    it('should extract resources with correct metadata', () => {
      const result = extractResourcesSummary(MOCK_SUCCESS_REPORT_DATA_POLICY)

      expect(result).toHaveLength(2)

      // Schema
      expect(result[0]).toEqual({
        id: 'temperature-schema',
        version: expect.any(Number),
        type: 'SCHEMA',
        isNew: true,
        metadata: {
          schemaType: 'JSON',
        },
      })

      // Script
      expect(result[1]).toEqual({
        id: 'transform-temperature',
        version: expect.any(Number),
        type: 'FUNCTION',
        isNew: true,
        metadata: {
          functionType: 'TRANSFORMATION',
        },
      })
    })

    it('should distinguish between new and modified resources', () => {
      const result = extractResourcesSummary(MOCK_SUCCESS_REPORT_MIXED_RESOURCES)

      expect(result).toHaveLength(2)
      expect(result[0].isNew).toBe(true) // DRAFT version
      expect(result[1].isNew).toBe(false) // MODIFIED version
    })

    it.each([
      { name: 'report with no resources', report: MOCK_SUCCESS_REPORT_NO_RESOURCES },
      { name: 'empty report', report: MOCK_EMPTY_REPORT },
      { name: 'undefined report', report: undefined },
    ])('should handle $name', ({ report }) => {
      const result = extractResourcesSummary(report)

      expect(result).toEqual([])
    })

    it('should filter out non-resource nodes', () => {
      const reportWithMixedNodes: DryRunResults<unknown, never>[] = [
        ...MOCK_SUCCESS_REPORT_DATA_POLICY.slice(0, -1), // Per-node items
        {
          ...MOCK_SUCCESS_REPORT_DATA_POLICY[MOCK_SUCCESS_REPORT_DATA_POLICY.length - 1],
          resources: [
            ...(MOCK_SUCCESS_REPORT_DATA_POLICY[MOCK_SUCCESS_REPORT_DATA_POLICY.length - 1].resources || []),
            {
              node: {
                id: 'operation-1',
                type: DataHubNodeType.OPERATION, // Not a resource
                position: { x: 0, y: 0 },
                data: {},
              },
              data: undefined,
              error: undefined,
            },
          ],
        },
      ]

      const result = extractResourcesSummary(reportWithMixedNodes)

      // Should only extract SCHEMA and FUNCTION types
      expect(result).toHaveLength(2)
      expect(result.every((r) => r.type === 'SCHEMA' || r.type === 'FUNCTION')).toBe(true)
    })
  })

  describe('extractPolicyPayload', () => {
    it('should extract complete policy payload with resources', () => {
      const result = extractPolicyPayload(MOCK_SUCCESS_REPORT_DATA_POLICY)

      expect(result).toBeDefined()
      expect(result?.policy).toBeDefined()
      expect(result?.resources.schemas).toHaveLength(1)
      expect(result?.resources.scripts).toHaveLength(1)
    })

    it('should extract policy payload without resources', () => {
      const result = extractPolicyPayload(MOCK_SUCCESS_REPORT_NO_RESOURCES)

      expect(result).toBeDefined()
      expect(result?.policy).toBeDefined()
      expect(result?.resources.schemas).toEqual([])
      expect(result?.resources.scripts).toEqual([])
    })

    it.each([
      { name: 'empty report', report: MOCK_EMPTY_REPORT },
      { name: 'undefined report', report: undefined },
    ])('should handle $name', ({ report }) => {
      const result = extractPolicyPayload(report)

      expect(result).toBeUndefined()
    })

    it('should separate schemas and scripts correctly', () => {
      const result = extractPolicyPayload(MOCK_SUCCESS_REPORT_MIXED_RESOURCES)

      expect(result).toBeDefined()
      expect(result?.resources.schemas).toHaveLength(2)
      expect(result?.resources.scripts).toEqual([])
    })
  })

  describe('groupResourcesByType', () => {
    it('should group resources by type', () => {
      const resources = extractResourcesSummary(MOCK_SUCCESS_REPORT_DATA_POLICY)
      const result = groupResourcesByType(resources)

      expect(result.schemas).toHaveLength(1)
      expect(result.scripts).toHaveLength(1)
      expect(result.schemas[0].type).toBe('SCHEMA')
      expect(result.scripts[0].type).toBe('FUNCTION')
    })

    it.each([
      {
        name: 'empty resource array',
        resources: [],
        expectedSchemas: 0,
        expectedScripts: 0,
      },
      {
        name: 'only schemas',
        resources: extractResourcesSummary(MOCK_SUCCESS_REPORT_MIXED_RESOURCES),
        expectedSchemas: 2,
        expectedScripts: 0,
      },
      {
        name: 'only scripts',
        resources: [
          {
            id: 'script-1',
            version: 1,
            type: 'FUNCTION' as const,
            isNew: true,
            metadata: { functionType: 'TRANSFORMATION' },
          },
        ],
        expectedSchemas: 0,
        expectedScripts: 1,
      },
    ])('should handle $name', ({ resources, expectedSchemas, expectedScripts }) => {
      const result = groupResourcesByType(resources)

      expect(result.schemas).toHaveLength(expectedSchemas)
      expect(result.scripts).toHaveLength(expectedScripts)
    })
  })

  describe('Integration tests', () => {
    it('should work together for complete flow - Data Policy', () => {
      const policySummary = extractPolicySummary(MOCK_SUCCESS_REPORT_DATA_POLICY, DesignerStatus.DRAFT)
      const resources = extractResourcesSummary(MOCK_SUCCESS_REPORT_DATA_POLICY)
      const payload = extractPolicyPayload(MOCK_SUCCESS_REPORT_DATA_POLICY)
      const grouped = groupResourcesByType(resources)

      // Policy summary
      expect(policySummary?.id).toBe('my-data-policy')
      expect(policySummary?.isNew).toBe(true)
      expect(policySummary?.topicFilters).toHaveLength(1) // API uses singular topicFilter

      // Resources
      expect(resources).toHaveLength(2)
      expect(grouped.schemas).toHaveLength(1)
      expect(grouped.scripts).toHaveLength(1)

      // Payload
      expect(payload?.policy).toBeDefined()
      expect(payload?.resources.schemas).toHaveLength(1)
      expect(payload?.resources.scripts).toHaveLength(1)
    })

    it('should work together for complete flow - Behavior Policy', () => {
      const policySummary = extractPolicySummary(MOCK_SUCCESS_REPORT_BEHAVIOR_POLICY, DesignerStatus.MODIFIED)
      const resources = extractResourcesSummary(MOCK_SUCCESS_REPORT_BEHAVIOR_POLICY)
      const payload = extractPolicyPayload(MOCK_SUCCESS_REPORT_BEHAVIOR_POLICY)

      // Policy summary
      expect(policySummary?.id).toBe('my-behavior-policy')
      expect(policySummary?.isNew).toBe(false) // MODIFIED = update
      expect(policySummary?.transitions).toEqual(['Mqtt.OnInboundPublish'])

      // No resources for this policy
      expect(resources).toEqual([])
      expect(payload?.resources.schemas).toEqual([])
      expect(payload?.resources.scripts).toEqual([])
    })
  })
})
