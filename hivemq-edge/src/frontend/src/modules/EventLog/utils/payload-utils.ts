export const prettyJSON = (payload: string | undefined) => {
  if (!payload) return null
  try {
    return JSON.stringify(JSON.parse(payload), null, 2)
  } catch (e) {
    return null
  }
}

export const prettifyXml = (sourceXml: string | undefined) => {
  if (!sourceXml) return null

  const xmlDoc = new DOMParser().parseFromString(sourceXml, 'application/xml')
  const xsltDoc = new DOMParser().parseFromString(
    [
      // describes how we want to modify the XML - indent everything
      '<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1">',
      '  <xsl:strip-space elements="*"/>',
      '  <xsl:template match="para[content-style][not(text())]">', // change to just text() to strip space in text nodes
      '    <xsl:value-of select="normalize-space(.)"/>',
      '  </xsl:template>',
      '  <xsl:template match="node()|@*">',
      '    <xsl:copy><xsl:apply-templates select="node()|@*"/></xsl:copy>',
      '  </xsl:template>',
      '  <xsl:output indent="yes"/>',
      '</xsl:stylesheet>',
    ].join('\n'),
    'application/xml'
  )

  try {
    const xsltProcessor = new XSLTProcessor()
    xsltProcessor.importStylesheet(xsltDoc)
    const resultDoc = xsltProcessor.transformToDocument(xmlDoc)
    return new XMLSerializer().serializeToString(resultDoc)
  } catch (e) {
    return null
  }
}
