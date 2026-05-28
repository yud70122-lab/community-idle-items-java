const HIGHLIGHT_REGEX = /<em>(.*?)<\/em>/g

function parseHighlight(html) {
  if (!html) {
    return []
  }

  const nodes = []
  let lastIndex = 0
  let match

  while ((match = HIGHLIGHT_REGEX.exec(html)) !== null) {
    if (match.index > lastIndex) {
      nodes.push({
        type: 'text',
        text: html.substring(lastIndex, match.index)
      })
    }

    nodes.push({
      type: 'html',
      text: match[0]
    })

    lastIndex = HIGHLIGHT_REGEX.lastIndex
  }

  if (lastIndex < html.length) {
    nodes.push({
      type: 'text',
      text: html.substring(lastIndex)
    })
  }

  return nodes
}

function convertToRichText(html, highlightColor = '#ff6b35') {
  if (!html) {
    return ''
  }

  return html.replace(
    /<em>(.*?)<\/em>/g,
    `<span style="color: ${highlightColor}; font-weight: bold;">$1</span>`
  )
}

function extractHighlightText(html) {
  if (!html) {
    return ''
  }

  return html.replace(/<em>/g, '').replace(/<\/em>/g, '')
}

function hasHighlight(html) {
  if (!html) {
    return false
  }

  return HIGHLIGHT_REGEX.test(html)
}

function getDisplayTitle(item) {
  if (item.highlightTitle && hasHighlight(item.highlightTitle)) {
    return item.highlightTitle
  }
  return item.title
}

function getDisplayDescription(item, maxLength = 80) {
  if (item.highlightDescription && hasHighlight(item.highlightDescription)) {
    return item.highlightDescription
  }

  if (item.description && item.description.length > maxLength) {
    return item.description.substring(0, maxLength) + '...'
  }

  return item.description
}

module.exports = {
  parseHighlight,
  convertToRichText,
  extractHighlightText,
  hasHighlight,
  getDisplayTitle,
  getDisplayDescription
}
