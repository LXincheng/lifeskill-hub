import { useMemo, useState } from 'react'

import { Icon } from '../components/Icon'
import type { IconName } from '../components/Icon'
import type { ContentItem, ContentItemType } from './learningApi'

export const contentTypeConfig: Record<ContentItemType, { label: string; group: string; icon: IconName; tone: string }> = {
  LEARNING_PATH: { label: '学习路径', group: '学习路径', icon: 'route', tone: 'blue' },
  ARTICLE: { label: '文章', group: '文章', icon: 'file', tone: 'orange' },
  NOTE: { label: '笔记', group: '笔记', icon: 'note', tone: 'purple' },
  QUIZ: { label: '测验', group: '测验', icon: 'file-question', tone: 'yellow' },
  CHECKLIST: { label: '行动清单', group: '行动清单', icon: 'list', tone: 'green' },
}

export const contentTypeOrder: ContentItemType[] = ['LEARNING_PATH', 'ARTICLE', 'NOTE', 'QUIZ', 'CHECKLIST']

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

export function LearningContentViewer({ item, onEdit }: { item: ContentItem; onEdit: () => void }) {
  if (item.type === 'LEARNING_PATH' || item.type === 'CHECKLIST') return <PathViewer item={item} onEdit={onEdit} />
  if (item.type === 'QUIZ') return <QuizViewer item={item} onEdit={onEdit} />
  return <ArticleReader item={item} onEdit={onEdit} />
}

function ContentHeader({ item, onEdit }: { item: ContentItem; onEdit: () => void }) {
  const config = contentTypeConfig[item.type]
  return <header className="content-header"><div className="content-meta"><span className={`file-type-icon ${config.tone}`}><Icon name={config.icon} size={17} /></span><span>{config.label} · 更新于 {formatDate(item.updatedAt)}</span></div><button className="secondary-button" onClick={onEdit}><Icon name="pencil" size={15} />编辑</button></header>
}

function ArticleReader({ item, onEdit }: { item: ContentItem; onEdit: () => void }) {
  const blocks = item.body.split('```')
  return <article className="content-view article-reader"><ContentHeader item={item} onEdit={onEdit} /><h1>{item.title}</h1><div className="article-body">{blocks.map((block, index) => index % 2 === 1 ? <pre key={index}><code>{block.trim()}</code></pre> : block.split(/\n\s*\n/).filter(Boolean).map((paragraph, paragraphIndex) => <p key={`${index}-${paragraphIndex}`}>{paragraph.trim()}</p>))}</div></article>
}

function PathViewer({ item, onEdit }: { item: ContentItem; onEdit: () => void }) {
  const steps = item.body.split('\n').map((line) => line.trim()).filter(Boolean).map((line) => ({ done: /^\[x\]/i.test(line), text: line.replace(/^\[(?:x| )\]\s*/i, '').replace(/^[-*\d.]+\s*/, '') }))
  const completed = steps.filter((step) => step.done).length
  const currentIndex = steps.findIndex((step) => !step.done)
  return <article className="content-view path-viewer"><ContentHeader item={item} onEdit={onEdit} /><h1>{item.title}</h1><p className="content-intro">按顺序推进，每完成一步就在编辑页使用 [x] 标记。</p><div className="path-progress"><span><strong>{completed}</strong> / {steps.length} 已完成</span><i><b style={{ width: `${steps.length ? completed / steps.length * 100 : 0}%` }} /></i></div><div className="timeline">{steps.map((step, index) => <div className={step.done ? 'timeline-step done' : index === currentIndex ? 'timeline-step current' : 'timeline-step'} key={`${step.text}-${index}`}><span className="timeline-marker">{step.done ? <Icon name="check" size={14} /> : index + 1}</span><div><small>{step.done ? '已完成' : index === currentIndex ? '当前步骤' : '待开始'}</small><strong>{step.text}</strong></div></div>)}</div></article>
}

type QuizQuestion = { prompt: string; options: string[]; answer: number }

function parseQuiz(body: string): QuizQuestion[] {
  return body.split(/\n\s*---\s*\n/).map((block) => {
    const lines = block.split('\n').map((line) => line.trim()).filter(Boolean)
    const prompt = lines.find((line) => !/^[-*]\s+/.test(line) && !/^(?:answer|答案)\s*[:：]/i.test(line)) ?? ''
    const options = lines.filter((line) => /^[-*]\s+/.test(line)).map((line) => line.replace(/^[-*]\s+/, ''))
    const answerLine = lines.find((line) => /^(?:answer|答案)\s*[:：]/i.test(line))
    const answer = answerLine ? Number(answerLine.match(/\d+/)?.[0] ?? 0) - 1 : -1
    return { prompt, options, answer }
  }).filter((question) => question.prompt && question.options.length >= 2 && question.answer >= 0 && question.answer < question.options.length)
}

function QuizViewer({ item, onEdit }: { item: ContentItem; onEdit: () => void }) {
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
