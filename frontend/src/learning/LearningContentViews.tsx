import { useEffect, useMemo, useState } from 'react'

import { Icon } from '../components/Icon'
import type { IconName } from '../components/Icon'
import { addAnnotation, deleteAnnotation, listAnnotations, listAttempts, recordAttempt, regenerateContent } from './learningApi'
import type { ContentItem, ContentItemType } from './learningApi'
import { RichText } from './RichText'

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

export function LearningContentViewer({ item, onEdit, onProgressChange, onContentChange }: { item: ContentItem; onEdit?: () => void; onProgressChange?: () => void; onContentChange?: (item: ContentItem) => void }) {
  if (item.type === 'REPORT') return <ProfessionalReportReader item={item} onEdit={onEdit} onProgressChange={onProgressChange} onContentChange={onContentChange} />
  if (item.type === 'LEARNING_PATH' || item.type === 'CHECKLIST') return <PathViewer item={item} onEdit={onEdit} onProgressChange={onProgressChange} />
  if (item.type === 'QUIZ') return <QuizViewer item={item} onEdit={onEdit} onProgressChange={onProgressChange} />
  return <ArticleReader item={item} onEdit={onEdit} onProgressChange={onProgressChange} onContentChange={onContentChange} />
}

function ProfessionalReportReader({ item, onEdit, onProgressChange, onContentChange }: { item: ContentItem; onEdit?: () => void; onProgressChange?: () => void; onContentChange?: (item: ContentItem) => void }) {
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
    <ReadingActions item={item} onContentChange={onContentChange} />
    <CompletionControl item={item} onProgressChange={onProgressChange} />
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
  return <div className="report-prose"><RichText body={body} /></div>
}

function ContentHeader({ item, onEdit }: { item: ContentItem; onEdit?: () => void }) {
  const config = contentTypeConfig[item.type]
  const canEdit = onEdit && item.verificationStatus === 'USER_AUTHORED' && item.type !== 'ARTICLE' && item.type !== 'REPORT'
  return <header className="content-header"><div className="content-meta"><span className={`file-type-icon ${config.tone}`}><Icon name={config.icon} size={17} /></span><span>{config.label} · 更新于 {formatDate(item.updatedAt)}</span></div>{canEdit && <button className="secondary-button" onClick={onEdit}><Icon name="pencil" size={15} />编辑</button>}</header>
}

function ArticleReader({ item, onEdit, onProgressChange, onContentChange }: { item: ContentItem; onEdit?: () => void; onProgressChange?: () => void; onContentChange?: (item: ContentItem) => void }) {
  return <article className="content-view article-reader"><ContentHeader item={item} onEdit={onEdit} /><h1>{item.title}</h1><div className="article-body"><RichText body={item.body} /></div><ReadingActions item={item} onContentChange={onContentChange} /><CompletionControl item={item} onProgressChange={onProgressChange} /></article>
}

function PathViewer({ item, onEdit, onProgressChange }: { item: ContentItem; onEdit?: () => void; onProgressChange?: () => void }) {
  const lines = item.body.split('\n').map((line) => line.trim()).filter(Boolean)
  const headingIndexes = lines.map((line, index) => /^#{1,3}\s*步骤\s*\d+/i.test(line) ? index : -1).filter((index) => index >= 0)
  const rawSteps = headingIndexes.length > 0 ? headingIndexes.map((start, index) => {
    const end = headingIndexes[index + 1] ?? lines.length
    return lines.slice(start, end).join(' ').replace(/^#{1,3}\s*/, '')
  }) : lines
  const steps = rawSteps.map((line) => {
    const normalized = line.replace(/^(?:[-*]|\d+[.)])\s*/, '')
    return { done: /^\[x\]/i.test(normalized), text: normalized.replace(/^\[(?:x| )\]\s*/i, '') }
  })
  const [completedIndexes, setCompletedIndexes] = useState<number[]>(steps.map((step, index) => step.done ? index : -1).filter((index) => index >= 0))
  useEffect(() => { void listAttempts(item.id).then((attempts) => {
    const latest = attempts.find((attempt) => attempt.kind === 'PROGRESS')
    if (latest) setCompletedIndexes(latest.completedUnitIndexes)
  }) }, [item.id])
  const completed = completedIndexes.length
  const currentIndex = steps.findIndex((_, index) => !completedIndexes.includes(index))
  async function toggleStep(index: number) {
    const next = completedIndexes.includes(index) ? completedIndexes.filter((value) => value !== index) : [...completedIndexes, index].sort((a, b) => a - b)
    setCompletedIndexes(next)
    await recordAttempt(item.id, { kind: 'PROGRESS', status: next.length === steps.length ? 'COMPLETED' : 'IN_PROGRESS', completedUnits: next.length, totalUnits: steps.length, completedUnitIndexes: next })
    onProgressChange?.()
  }
  return <article className="content-view path-viewer"><ContentHeader item={item} onEdit={onEdit} /><h1>{item.title}</h1><p className="content-intro">按顺序推进。点击步骤即可记录完成状态，刷新后仍会保留。</p><div className="path-progress"><span><strong>{completed}</strong> / {steps.length} 已完成</span><i><b style={{ width: `${steps.length ? completed / steps.length * 100 : 0}%` }} /></i></div><div className="timeline">{steps.map((step, index) => { const done = completedIndexes.includes(index); return <button className={done ? 'timeline-step done' : index === currentIndex ? 'timeline-step current' : 'timeline-step'} key={`${step.text}-${index}`} onClick={() => void toggleStep(index)}><span className="timeline-marker">{done ? <Icon name="check" size={14} /> : index + 1}</span><div><small>{done ? '已完成' : index === currentIndex ? '当前步骤' : '待开始'}</small><strong>{step.text}</strong></div></button> })}</div></article>
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

function QuizViewer({ item, onEdit, onProgressChange }: { item: ContentItem; onEdit?: () => void; onProgressChange?: () => void }) {
  const parsedQuestions = useMemo(() => parseQuiz(item.body), [item.body])
  const [questions, setQuestions] = useState(parsedQuestions)
  const [current, setCurrent] = useState(0)
  const [selected, setSelected] = useState<number | null>(null)
  const [correctCount, setCorrectCount] = useState(0)
  const [isComplete, setIsComplete] = useState(false)
  useEffect(() => { setQuestions(shuffleQuestions(parsedQuestions)); setCurrent(0); setSelected(null); setCorrectCount(0); setIsComplete(false) }, [item.id, parsedQuestions])

  if (questions.length === 0) return <article className="content-view quiz-viewer"><ContentHeader item={item} onEdit={onEdit} /><h1>{item.title}</h1><div className="quiz-format-empty"><Icon name="file-question" size={24} /><strong>还没有可识别的题目</strong><p>请编辑内容，按“题目、- 选项、答案: 1”的格式填写。</p></div></article>

  const question = questions[current]
  const score = Math.round(correctCount / questions.length * 100)

  async function handleNext() {
    if (selected === null) return
    const nextCorrectCount = correctCount + (selected === question.answer ? 1 : 0)
    setCorrectCount(nextCorrectCount)
    if (current === questions.length - 1) {
      setIsComplete(true)
      await recordAttempt(item.id, { kind: 'QUIZ', status: 'COMPLETED', completedUnits: nextCorrectCount, totalUnits: questions.length, completedUnitIndexes: [] })
      onProgressChange?.()
    }
    else { setCurrent((value) => value + 1); setSelected(null) }
  }

  function handleRestart() { setQuestions(shuffleQuestions(parsedQuestions)); setCurrent(0); setSelected(null); setCorrectCount(0); setIsComplete(false) }

  return <article className="content-view quiz-viewer"><ContentHeader item={item} onEdit={onEdit} /><h1>{item.title}</h1>{isComplete ? <div className="quiz-result"><span>{score}</span><strong>测验完成</strong><p>答对 {correctCount} / {questions.length} 题 · 结果已保存</p><button className="primary-button" onClick={handleRestart}>随机再测一次</button></div> : <div className="quiz-card"><div className="quiz-progress"><span>第 {current + 1} 题 / 共 {questions.length} 题</span><i><b style={{ width: `${current / questions.length * 100}%` }} /></i></div><h2>{question.prompt}</h2><div className="quiz-options">{question.options.map((option, index) => <button className={selected === index ? 'selected' : ''} key={option} onClick={() => setSelected(index)}><span>{String.fromCharCode(65 + index)}</span>{option}</button>)}</div><button className="primary-button quiz-next" disabled={selected === null} onClick={() => void handleNext()}>{current === questions.length - 1 ? '提交测验' : '下一题'}</button></div>}</article>
}

function shuffleQuestions(questions: QuizQuestion[]) {
  const shuffled = [...questions]
  for (let index = shuffled.length - 1; index > 0; index--) {
    const target = Math.floor(Math.random() * (index + 1))
    const current = shuffled[index]
    shuffled[index] = shuffled[target]
    shuffled[target] = current
  }
  return shuffled
}

function CompletionControl({ item, onProgressChange }: { item: ContentItem; onProgressChange?: () => void }) {
  const [completed, setCompleted] = useState(false)
  useEffect(() => { void listAttempts(item.id).then((attempts) => setCompleted(attempts.some((attempt) => attempt.kind === 'PROGRESS' && attempt.status === 'COMPLETED'))) }, [item.id])
  return <button className={completed ? 'reading-complete completed' : 'reading-complete'} disabled={completed} onClick={async () => {
    await recordAttempt(item.id, { kind: 'PROGRESS', status: 'COMPLETED', completedUnits: 1, totalUnits: 1, completedUnitIndexes: [0] })
    setCompleted(true)
    onProgressChange?.()
  }}><Icon name={completed ? 'check-circle' : 'bookmark'} size={17} />{completed ? '已完成阅读' : '标记为已读'}</button>
}

function ReadingActions({ item, onContentChange }: { item: ContentItem; onContentChange?: (item: ContentItem) => void }) {
  const [annotations, setAnnotations] = useState<Awaited<ReturnType<typeof listAnnotations>>>([])
  const [feedback, setFeedback] = useState('')
  const [notice, setNotice] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const load = () => listAnnotations(item.id).then(setAnnotations)
  useEffect(() => { void load() }, [item.id])

  async function saveHighlight() {
    const selectedText = window.getSelection()?.toString().trim() ?? ''
    if (!selectedText) { setNotice('请先在正文中选择一段文字。'); return }
    setBusy(true)
    try { await addAnnotation(item.id, { kind: 'HIGHLIGHT', selectedText }); await load(); setNotice('重点已保存。') }
    finally { setBusy(false) }
  }

  async function saveFeedback() {
    if (!feedback.trim()) return
    setBusy(true)
    try { await addAnnotation(item.id, { kind: 'FEEDBACK', note: feedback.trim() }); setFeedback(''); await load(); setNotice('建议已保存，可据此重新生成。') }
    finally { setBusy(false) }
  }

  async function regenerate() {
    setBusy(true)
    setNotice('Agent 正在根据反馈修订内容…')
    try { const revised = await regenerateContent(item.id); onContentChange?.(revised); setNotice('新版内容已生成，原反馈仍保留。') }
    catch (cause) { setNotice(cause instanceof Error ? cause.message : '重新生成失败。') }
    finally { setBusy(false) }
  }

  return <section className="reading-actions">
    <header><div><small>阅读工作台</small><strong>标记重点或告诉 Agent 如何改进</strong></div><button className="secondary-button" disabled={busy} onClick={() => void saveHighlight()}><Icon name="highlight" size={15} />标记选中文字</button></header>
    <div className="feedback-composer"><textarea maxLength={2000} onChange={(event) => setFeedback(event.target.value)} placeholder="例如：增加一个具体案例，删去重复段落，解释这个术语……" rows={2} value={feedback} /><button disabled={busy || !feedback.trim()} onClick={() => void saveFeedback()}><Icon name="message-circle" size={15} />提交建议</button></div>
    {notice && <p className="reading-notice">{notice}</p>}
    {annotations.length > 0 && <div className="annotation-list">{annotations.map((annotation) => <article key={annotation.id}><span><Icon name={annotation.kind === 'HIGHLIGHT' ? 'highlight' : 'message-circle'} size={14} /></span><p>{annotation.selectedText ?? annotation.note}</p><button aria-label="删除标记" onClick={async () => { await deleteAnnotation(item.id, annotation.id); await load() }}><Icon name="x" size={13} /></button></article>)}</div>}
    {item.verificationStatus === 'AI_GENERATED' && <button className="regenerate-button" disabled={busy || !annotations.some((annotation) => annotation.kind === 'FEEDBACK')} onClick={() => void regenerate()}><Icon name="refresh" size={15} />根据全部建议重新生成</button>}
  </section>
}
