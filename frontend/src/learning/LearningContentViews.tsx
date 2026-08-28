import { useMemo, useState } from 'react'

import { Icon } from '../components/Icon'
import type { IconName } from '../components/Icon'
import type { ContentItem, ContentItemType } from './learningApi'

export const contentTypeConfig: Record<ContentItemType, { label: string; group: string; icon: IconName; tone: string }> = {
  REPORT: { label: '研究报告', group: '研究报告', icon: 'file', tone: 'red' },
  LEARNING_PATH: { label: '学习路径', group: '学习路径', icon: 'route', tone: 'blue' },
  ARTICLE: { label: '文章', group: '文章', icon: 'file', tone: 'orange' },
  NOTE: { label: '笔记', group: '笔记', icon: 'note', tone: 'purple' },
  QUIZ: { label: '测验', group: '测验', icon: 'file-question', tone: 'yellow' },
  CHECKLIST: { label: '行动清单', group: '行动清单', icon: 'list', tone: 'green' },
}

export const contentTypeOrder: ContentItemType[] = ['REPORT', 'LEARNING_PATH', 'ARTICLE', 'NOTE', 'QUIZ', 'CHECKLIST']

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

export function LearningContentViewer({ item, onEdit }: { item: ContentItem; onEdit?: () => void }) {
  if (item.type === 'REPORT') return <ProfessionalReportReader item={item} onEdit={onEdit} />
  if (item.type === 'LEARNING_PATH' || item.type === 'CHECKLIST') return <PathViewer item={item} onEdit={onEdit} />
  if (item.type === 'QUIZ') return <QuizViewer item={item} onEdit={onEdit} />
  return <ArticleReader item={item} onEdit={onEdit} />
}

function ProfessionalReportReader({ item, onEdit }: { item: ContentItem; onEdit?: () => void }) {
  const sections = item.body.split(/^##\s+/m)
  const lead = sections.shift()?.trim().replace(/^>\s*/, '') ?? ''
  return <article className="content-view professional-report">
    <ContentHeader item={item} onEdit={onEdit} />
    <header className="report-cover"><span>专项研究 · 已核验 · {formatDate(item.updatedAt)}</span><h1>{item.title}</h1><p>{lead}</p><div><small>数据基础</small><strong>World Gold Council 官方研究</strong><small>报告性质</small><strong>条件式研究，不构成投资建议</strong></div></header>
    <div className="report-sections">{sections.map((section, index) => {
      const [heading, ...bodyLines] = section.split('\n')
      const body = bodyLines.join('\n').trim()
      return <section key={`${heading}-${index}`}><h2><em>{String(index + 1).padStart(2, '0')}</em><span>{heading.replace(/^\d+\s*/, '')}</span></h2>{renderReportBody(body, index)}</section>
    })}</div>
    <footer className="report-disclaimer"><strong>口径说明</strong><span>报告只复述并整理已保存的官方 Evidence；市场信息具有时效性，请在决策前重新核验。</span></footer>
  </article>
}

function renderReportBody(body: string, sectionIndex: number) {
  const lines = body.split('\n').map((line) => line.trim()).filter(Boolean)
  const tableLines = lines.filter((line) => line.startsWith('|'))
  if (tableLines.length >= 3) {
    const rows = tableLines.filter((_, index) => index !== 1).map((line) => line.split('|').slice(1, -1).map((cell) => cell.trim()))
    return <div className="report-table-wrap"><table><thead><tr>{rows[0].map((cell) => <th key={cell}>{cell}</th>)}</tr></thead><tbody>{rows.slice(1).map((row, rowIndex) => <tr key={rowIndex}>{row.map((cell, cellIndex) => <td key={`${rowIndex}-${cellIndex}`}>{cellIndex === 2 ? <span className="report-direction">{cell}</span> : cell}</td>)}</tr>)}</tbody></table></div>
  }
  const items = lines.filter((line) => /^[-*]\s+|^\d+[.、]\s*/.test(line))
  if (items.length > 0) return <ol className={sectionIndex === 3 ? 'report-timeline' : 'report-points'}>{items.map((line, index) => <li key={`${line}-${index}`}><span>{String(index + 1).padStart(2, '0')}</span><p>{line.replace(/^[-*]\s+|^\d+[.、]\s*/, '')}</p></li>)}</ol>
  return <div className="report-prose">{body.split(/\n\s*\n/).filter(Boolean).map((paragraph, index) => <p key={index}>{paragraph}</p>)}</div>
}

function ContentHeader({ item, onEdit }: { item: ContentItem; onEdit?: () => void }) {
  const config = contentTypeConfig[item.type]
  return <header className="content-header"><div className="content-meta"><span className={`file-type-icon ${config.tone}`}><Icon name={config.icon} size={17} /></span><span>{config.label} · 更新于 {formatDate(item.updatedAt)}</span></div>{onEdit && <button className="secondary-button" onClick={onEdit}><Icon name="pencil" size={15} />编辑</button>}</header>
}

function ArticleReader({ item, onEdit }: { item: ContentItem; onEdit?: () => void }) {
  const blocks = item.body.split('```')
  return <article className="content-view article-reader"><ContentHeader item={item} onEdit={onEdit} /><h1>{item.title}</h1><div className="article-body">{blocks.map((block, index) => index % 2 === 1 ? <pre key={index}><code>{block.trim()}</code></pre> : renderArticleText(block, index))}</div></article>
}

function renderArticleText(block: string, blockIndex: number) {
  return block.split(/\n\s*\n/).filter(Boolean).map((raw, paragraphIndex) => {
    const paragraph = raw.trim()
    const image = paragraph.match(/^!\[([^\]]*)\]\((https?:\/\/[^\s)]+)\)$/i)
    if (image) {
      return <figure className="article-media" key={`${blockIndex}-${paragraphIndex}`}><img src={image[2]} alt={image[1]} loading="lazy" /><figcaption>{image[1]}</figcaption></figure>
    }
    if (paragraph.startsWith('## ')) return <h2 key={`${blockIndex}-${paragraphIndex}`}>{paragraph.slice(3)}</h2>
    return <p key={`${blockIndex}-${paragraphIndex}`}>{paragraph}</p>
  })
}

function PathViewer({ item, onEdit }: { item: ContentItem; onEdit?: () => void }) {
  const lines = item.body.split('\n').map((line) => line.trim()).filter(Boolean)
  const headingIndexes = lines.map((line, index) => /^#{1,3}\s*步骤\s*\d+/i.test(line) ? index : -1).filter((index) => index >= 0)
  const rawSteps = headingIndexes.length > 0 ? headingIndexes.map((start, index) => {
    const end = headingIndexes[index + 1] ?? lines.length
    return lines.slice(start, end).join(' ').replace(/^#{1,3}\s*/, '')
  }) : lines
  const steps = rawSteps.map((line) => ({ done: /^\[x\]/i.test(line), text: line.replace(/^\[(?:x| )\]\s*/i, '').replace(/^[-*\d.]+\s*/, '') }))
  const completed = steps.filter((step) => step.done).length
  const currentIndex = steps.findIndex((step) => !step.done)
  return <article className="content-view path-viewer"><ContentHeader item={item} onEdit={onEdit} /><h1>{item.title}</h1><p className="content-intro">按顺序推进，每完成一步就在编辑页使用 [x] 标记。</p><div className="path-progress"><span><strong>{completed}</strong> / {steps.length} 已完成</span><i><b style={{ width: `${steps.length ? completed / steps.length * 100 : 0}%` }} /></i></div><div className="timeline">{steps.map((step, index) => <div className={step.done ? 'timeline-step done' : index === currentIndex ? 'timeline-step current' : 'timeline-step'} key={`${step.text}-${index}`}><span className="timeline-marker">{step.done ? <Icon name="check" size={14} /> : index + 1}</span><div><small>{step.done ? '已完成' : index === currentIndex ? '当前步骤' : '待开始'}</small><strong>{step.text}</strong></div></div>)}</div></article>
}

type QuizQuestion = { prompt: string; options: string[]; answer: number }

function parseQuiz(body: string): QuizQuestion[] {
  const strict = body.split(/\n\s*---\s*\n/).map((block) => {
    const lines = block.split('\n').map((line) => line.trim()).filter(Boolean)
    const prompt = lines.find((line) => !/^[-*]\s+/.test(line) && !/^(?:answer|答案)\s*[:：]/i.test(line)) ?? ''
    const options = lines.filter((line) => /^[-*]\s+/.test(line)).map((line) => line.replace(/^[-*]\s+/, ''))
    const answerLine = lines.find((line) => /^(?:answer|答案)\s*[:：]/i.test(line))
    const answer = answerLine ? Number(answerLine.match(/\d+/)?.[0] ?? 0) - 1 : -1
    return { prompt, options, answer }
  }).filter((question) => question.prompt && question.options.length >= 2 && question.answer >= 0 && question.answer < question.options.length)
  if (strict.length > 0) return strict

  const lines = body.split('\n').map((line) => line.trim()).filter(Boolean)
  const headings = lines.map((line, index) => /^#{1,3}\s*题目\s*\d+/i.test(line) ? index : -1).filter((index) => index >= 0)
  const answerLine = lines.find((line) => /^答案\s*[:：]/.test(line)) ?? ''
  const answers = new Map(Array.from(answerLine.matchAll(/(\d+)\s*[.、:]\s*([A-D])/gi)).map((match) => [Number(match[1]), match[2].toUpperCase().charCodeAt(0) - 65]))
  return headings.map((start, index) => {
    const end = headings[index + 1] ?? lines.findIndex((line, lineIndex) => lineIndex > start && /^---$/.test(line))
    const block = lines.slice(start + 1, end > start ? end : lines.length)
    const prompt = block.find((line) => !/^[A-D][.、]\s*/i.test(line)) ?? ''
    const options = block.filter((line) => /^[A-D][.、]\s*/i.test(line)).map((line) => line.replace(/^[A-D][.、]\s*/i, ''))
    return { prompt, options, answer: answers.get(index + 1) ?? -1 }
  }).filter((question) => question.prompt && question.options.length >= 2 && question.answer >= 0 && question.answer < question.options.length)
}

function QuizViewer({ item, onEdit }: { item: ContentItem; onEdit?: () => void }) {
  const questions = useMemo(() => parseQuiz(item.body), [item.body])
  const [current, setCurrent] = useState(0)
  const [selected, setSelected] = useState<number | null>(null)
  const [correctCount, setCorrectCount] = useState(0)
  const [isComplete, setIsComplete] = useState(false)

  if (questions.length === 0) return <article className="content-view quiz-viewer"><ContentHeader item={item} onEdit={onEdit} /><h1>{item.title}</h1><div className="quiz-format-empty"><Icon name="file-question" size={24} /><strong>还没有可识别的题目</strong><p>请编辑内容，按“题目、- 选项、答案: 1”的格式填写。</p></div></article>

  const question = questions[current]
  const score = Math.round(correctCount / questions.length * 100)

  function handleNext() {
    if (selected === null) return
    const nextCorrectCount = correctCount + (selected === question.answer ? 1 : 0)
    setCorrectCount(nextCorrectCount)
    if (current === questions.length - 1) setIsComplete(true)
    else { setCurrent((value) => value + 1); setSelected(null) }
  }

  function handleRestart() { setCurrent(0); setSelected(null); setCorrectCount(0); setIsComplete(false) }

  return <article className="content-view quiz-viewer"><ContentHeader item={item} onEdit={onEdit} /><h1>{item.title}</h1>{isComplete ? <div className="quiz-result"><span>{score}</span><strong>测验完成</strong><p>答对 {correctCount} / {questions.length} 题</p><button className="primary-button" onClick={handleRestart}>再做一次</button></div> : <div className="quiz-card"><div className="quiz-progress"><span>第 {current + 1} 题 / 共 {questions.length} 题</span><i><b style={{ width: `${current / questions.length * 100}%` }} /></i></div><h2>{question.prompt}</h2><div className="quiz-options">{question.options.map((option, index) => <button className={selected === index ? 'selected' : ''} key={option} onClick={() => setSelected(index)}><span>{String.fromCharCode(65 + index)}</span>{option}</button>)}</div><button className="primary-button quiz-next" disabled={selected === null} onClick={handleNext}>{current === questions.length - 1 ? '提交测验' : '下一题'}</button></div>}</article>
}
