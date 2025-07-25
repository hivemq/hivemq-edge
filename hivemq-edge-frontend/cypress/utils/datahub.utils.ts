import { datahubDesignerPage } from 'cypress/pages/DataHub/DesignerPage.ts'

const uuidPattern = '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'

export const nodeDataIdPattern = /^([A-Z_]+)_([a-f0-9-]+)/
export const nodeDataTestIdPattern = /^(rf__node)-([A-Z_]+)_([a-f0-9-]+)/
// export const edgeDataIdPattern = /^(xy-edge)__([A-Z_]+)_([a-f0-9-]+)-([A-Z_]+)_([a-f0-9-]+)/
export const edgeDataIdPattern = new RegExp(
  `^(xy-edge)__([A-Z_]+)_(${uuidPattern})([a-zA-Z0-9]*)-([A-Z_]+)_(${uuidPattern})([a-zA-Z0-9]*)`
)

/**
 * The loader has been tested, so JSON and nodes/edges should be in sync.
 * We are testing whether the graph is consistent
 */
export const cy_checkDataPolicyGraph = () => {
  let nodeIds: string[]
  let edgeIds: string[]

  datahubDesignerPage.designer
    .modes()
    .then(($elements) => {
      nodeIds = $elements.toArray().map((el) => el.getAttribute('data-id'))
    })
    .then(() => {
      datahubDesignerPage.designer.edges().then(($elements) => {
        edgeIds = $elements.toArray().map((el) => el.getAttribute('data-id'))
        const payload = { nodeIds, edgeIds }
        cy.wrap(payload).as('dataIds')
      })
    })
    .then(() => {
      const connectedNodes = edgeIds.reduce<string[]>((acc, id) => {
        const match = id.match(edgeDataIdPattern)
        if (match) {
          // eslint-disable-next-line @typescript-eslint/no-unused-vars
          const [_, prefix, type1, uuid1, handle1, type2, uuid2, handle2] = match
          acc.push(`${type1}_${uuid1}`, `${type2}_${uuid2}`)
        } else console.error('Cannot parse the node id', id)

        return acc
      }, [])

      const uniq = new Set(connectedNodes)

      const areArraysIdentical = (a: string[], b: string[]): boolean => {
        if (a.length !== b.length) return false
        const sortedA = [...a].sort()
        const sortedB = [...b].sort()
        return sortedA.every((val, idx) => val === sortedB[idx])
      }

      expect(areArraysIdentical(Array.from(uniq), nodeIds), 'The list of nodes should match the connected nodes').to.be
        .true
    })
}
