import type React from 'react'
import { useEffect, useRef, useState } from 'react'
import type { MermaidConfig } from 'mermaid'
import mermaid from 'mermaid'
import { Card, CardBody, useTheme, useColorMode } from '@chakra-ui/react'

export interface MermaidProps {
  text: string
  selectedTransitionIndex?: number
}

const DEFAULT_CONFIG: MermaidConfig = {
  startOnLoad: false,
  theme: 'base',
  logLevel: 'fatal',
  securityLevel: 'loose',
  arrowMarkerAbsolute: false,
  flowchart: {
    htmlLabels: true,
    curve: 'linear',
  },
  sequence: {
    diagramMarginX: 50,
    diagramMarginY: 10,
    actorMargin: 50,
    width: 150,
    height: 65,
    boxMargin: 10,
    boxTextMargin: 5,
    noteMargin: 10,
    messageMargin: 35,
    mirrorActors: true,
    bottomMarginAdj: 1,
    useMaxWidth: true,
    rightAngles: false,
    showSequenceNumbers: false,
  },
  gantt: {
    titleTopMargin: 25,
    barHeight: 20,
    barGap: 4,
    topPadding: 50,
    leftPadding: 75,
    gridLineStartPadding: 35,
    fontSize: 11,
    numberSectionStyles: 4,
    axisFormat: '%Y-%m-%d',
  },
}

let idCounter = 0

export const Mermaid: React.FC<MermaidProps> = ({ text, selectedTransitionIndex }) => {
  const containerRef = useRef<HTMLDivElement>(null)
  const [svg, setSvg] = useState<string>('')
  const theme = useTheme()
  const { colorMode } = useColorMode()

  useEffect(() => {
    mermaid.initialize({ ...DEFAULT_CONFIG })
  }, [])

  useEffect(() => {
    const renderDiagram = async () => {
      if (!text || !containerRef.current) return

      try {
        // Generate unique ID for each render to force re-render
        const id = `mermaid-${Date.now()}-${idCounter++}`

        console.log('Mermaid rendering with text:', text, 'colorMode:', colorMode, 'selectedIndex:', selectedTransitionIndex)

        // Render the diagram
        const { svg: renderedSvg } = await mermaid.render(id, text)

        // Post-process SVG to mark selected transition
        let processedSvg = renderedSvg
        if (selectedTransitionIndex !== undefined && selectedTransitionIndex >= 0) {
          // Parse SVG and find transition paths
          const parser = new DOMParser()
          const doc = parser.parseFromString(renderedSvg, 'image/svg+xml')
          const transitions = doc.querySelectorAll('path.transition')

          if (transitions[selectedTransitionIndex]) {
            transitions[selectedTransitionIndex].setAttribute('data-selected', 'true')
            processedSvg = new XMLSerializer().serializeToString(doc)
            console.log(`Marked transition ${selectedTransitionIndex} as selected`)
          }
        }

        setSvg(processedSvg)

        console.log('Mermaid rendered successfully')
      } catch (error) {
        console.error('Mermaid rendering error:', error)
        setSvg('')
      }
    }

    renderDiagram()
  }, [text, colorMode, selectedTransitionIndex]) // Re-render when color mode or selection changes

  // Get theme colors
  const textColor = colorMode === 'dark' ? theme.colors.whiteAlpha[900] : theme.colors.gray[800]
  const edgeColor = colorMode === 'dark' ? theme.colors.whiteAlpha[400] : theme.colors.gray[400]

  return (
    <Card>
      <CardBody
        sx={{
          '& svg': {
            scale: '0.75 0.75',
          },
          // Theme-aware text colors
          '& .edgeLabel text, & .edgeLabel span, & .edgeLabel p, & .edgeLabel div': {
            fill: `${textColor} !important`,
            color: `${textColor} !important`,
          },
          // Theme-aware transition lines (edges)
          '& .transition, & path.transition': {
            stroke: `${edgeColor} !important`,
          },
          // Selected transition path (using data attribute) - just thicker, same color
          '& path.transition[data-selected="true"]': {
            strokeWidth: '4px !important',
          },
          // Remove edge label backgrounds
          '& .edgeLabel rect': {
            fill: 'transparent !important',
            opacity: '0 !important',
          },
          '& .edgeLabel .label rect': {
            fill: 'transparent !important',
            opacity: '0 !important',
          },
          '& .edgeLabel': {
            backgroundColor: 'transparent !important',
          },
          '& .edgeLabel p': {
            backgroundColor: 'transparent !important',
          },
          // State labels should also respect theme
          '& .stateLabel text, & .nodeLabel': {
            fill: 'inherit !important',
          },
        }}
      >
        <div ref={containerRef} dangerouslySetInnerHTML={{ __html: svg }} />
      </CardBody>
    </Card>
  )
}
