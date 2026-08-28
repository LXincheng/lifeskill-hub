import { Fragment, useState } from 'react'
import type { ReactNode } from 'react'

import { Icon } from '../components/Icon'

type Block =
  | { kind: 'heading'; level: number; text: string }
  | { kind: 'paragraph'; text: string }
  | { kind: 'callout'; text: string }
  | { kind: 'code'; language: string; code: string }
  | { kind: 'list'; ordered: boolean; items: string[] }
  | { kind: 'table'; rows: string[][] }
  | { kind: 'image'; alt: string; url: string }

export function RichText({ body }: { body: string }) {
  const blocks = parseBlocks(body)
  return <div className="rich-text">{blocks.map((block, index) => <BlockView block={block} key={`${block.kind}-${index}`} />)}</div>
}

function BlockView({ block }: { block: Block }) {
  if (block.kind === 'heading') {
    const content = inline(block.text)
    return block.level <= 2 ? <h2>{content}</h2> : <h3>{content}</h3>
  }
  if (block.kind === 'callout') return <aside className="article-callout"><Icon name="sparkles" size={17} /><p>{inline(block.text)}</p></aside>
  if (block.kind === 'code') return <CodeBlock language={block.language} code={block.code} />
  if (block.kind === 'list') {
    const List = block.ordered ? 'ol' : 'ul'
    return <List>{block.items.map((item, index) => <li key={`${item}-${index}`}>{inline(item)}</li>)}</List>
  }
  if (block.kind === 'table') return <div className="rich-table-wrap"><table><thead><tr>{block.rows[0].map((cell, index) => <th key={`${cell}-${index}`}>{inline(cell)}</th>)}</tr></thead><tbody>{block.rows.slice(1).map((row, rowIndex) => <tr key={rowIndex}>{row.map((cell, cellIndex) => <td key={cellIndex}>{inline(cell)}</td>)}</tr>)}</tbody></table></div>
  if (block.kind === 'image') return <figure className="article-media"><img alt={block.alt} loading="lazy" src={block.url} /><figcaption>{block.alt}</figcaption></figure>
  return <p>{inline(block.text)}</p>
}

function CodeBlock({ language, code }: { language: string; code: string }) {
  const [copied, setCopied] = useState(false)
  return <section className="code-block"><header><span>{language || 'code'}</span><button onClick={async () => { await navigator.clipboard.writeText(code); setCopied(true); window.setTimeout(() => setCopied(false), 1200) }} type="button"><Icon name={copied ? 'check' : 'copy'} size={14} />{copied ? '已复制' : '复制'}</button></header><pre><code>{code}</code></pre></section>
}

function inline(text: string): ReactNode[] {
  const pattern = /(\[[^\]]+\]\(https?:\/\/[^\s)]+\)|`[^`]+`|\*\*[^*]+\*\*|(?<!\*)\*[^*]+\*(?!\*)|https?:\/\/[^\s)]+)/g
  return text.split(pattern).filter(Boolean).map((part, index) => {
    const link = part.match(/^\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)$/)
    if (link) return <a href={link[2]} key={index} rel="noreferrer" target="_blank">{link[1]}<Icon name="external-link" size={13} /></a>
    if (/^https?:\/\//.test(part)) return <a href={part} key={index} rel="noreferrer" target="_blank">{friendlyLink(part)}<Icon name="external-link" size={13} /></a>
    if (part.startsWith('`') && part.endsWith('`')) return <code key={index}>{part.slice(1, -1)}</code>
    if (part.startsWith('**') && part.endsWith('**')) return <strong key={index}>{part.slice(2, -2)}</strong>
    if (part.startsWith('*') && part.endsWith('*')) return <em key={index}>{part.slice(1, -1)}</em>
    return <Fragment key={index}>{part}</Fragment>
  })
}

function friendlyLink(value: string) {
  try {
    const url = new URL(value)
    const host = url.hostname.replace(/^www\./, '')
    const segment = decodeURIComponent(url.pathname.split('/').filter(Boolean).at(-1) ?? '')
      .replace(/[-_]+/g, ' ').replace(/\.[a-z0-9]+$/i, '')
    return segment ? `${host} · ${segment.slice(0, 42)}` : host
  } catch { return '查看来源' }
}

function parseBlocks(body: string): Block[] {
  const lines = body.replace(/\r/g, '').split('\n')
  const blocks: Block[] = []
  let index = 0
  while (index < lines.length) {
    const line = lines[index].trim()
    if (!line) { index++; continue }
    if (line.startsWith('```')) {
      const language = line.slice(3).trim()
      const code: string[] = []
      index++
      while (index < lines.length && !lines[index].trim().startsWith('```')) code.push(lines[index++])
      index++
      blocks.push({ kind: 'code', language, code: code.join('\n').trim() })
      continue
    }
    const image = line.match(/^!\[([^\]]*)\]\((https?:\/\/[^\s)]+)\)$/)
    if (image) { blocks.push({ kind: 'image', alt: image[1] || '学习资料图片', url: image[2] }); index++; continue }
    const heading = line.match(/^(#{1,4})\s+(.+)$/)
    if (heading) { blocks.push({ kind: 'heading', level: heading[1].length, text: heading[2] }); index++; continue }
    if (line.startsWith('>')) {
      const parts: string[] = []
      while (index < lines.length && lines[index].trim().startsWith('>')) parts.push(lines[index++].trim().replace(/^>\s?/, ''))
      blocks.push({ kind: 'callout', text: parts.join(' ') })
      continue
    }
    if (/^[-*]\s+/.test(line) || /^\d+[.、]\s*/.test(line)) {
      const ordered = /^\d+[.、]/.test(line)
      const items: string[] = []
      const matcher = ordered ? /^\d+[.、]\s*/ : /^[-*]\s+/
      while (index < lines.length && matcher.test(lines[index].trim())) items.push(lines[index++].trim().replace(matcher, ''))
      blocks.push({ kind: 'list', ordered, items })
      continue
    }
    if (line.startsWith('|') && index + 1 < lines.length && /^\|?[\s:|-]+\|?$/.test(lines[index + 1].trim())) {
      const rows: string[][] = [tableRow(line)]
      index += 2
      while (index < lines.length && lines[index].trim().startsWith('|')) rows.push(tableRow(lines[index++].trim()))
      blocks.push({ kind: 'table', rows })
      continue
    }
    const paragraph = [line]
    index++
    while (index < lines.length && lines[index].trim() && !/^(#{1,4})\s+|^```|^>|^[-*]\s+|^\d+[.、]\s*|^!\[|^\|/.test(lines[index].trim())) paragraph.push(lines[index++].trim())
    blocks.push({ kind: 'paragraph', text: paragraph.join(' ') })
  }
  return blocks
}

function tableRow(line: string) {
  return line.replace(/^\||\|$/g, '').split('|').map((cell) => cell.trim())
}
