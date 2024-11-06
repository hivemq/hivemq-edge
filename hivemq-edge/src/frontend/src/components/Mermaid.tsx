import React, { useEffect } from 'react'
import mermaid, { MermaidConfig } from 'mermaid'
import { Card, CardBody } from '@chakra-ui/react'

export interface MermaidProps {
  text: string
}

const DEFAULT_CONFIG: MermaidConfig = {
  startOnLoad: true,
  theme: 'neutral',
  logLevel: 'fatal',
  securityLevel: 'strict',
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

export const Mermaid: React.FC<MermaidProps> = ({ text }) => {
  useEffect(() => {
    mermaid.initialize({ ...DEFAULT_CONFIG })
  }, [])
  useEffect(() => {
    mermaid.contentLoaded()
  }, [text])

  return (
    <Card>
      <CardBody className="mermaid" sx={{ '& svg': { scale: '0.75 0.75' } }} data-processed={undefined}>
        {text}
      </CardBody>
    </Card>
  )
}
