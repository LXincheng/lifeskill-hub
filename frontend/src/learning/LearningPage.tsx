import { useCallback, useEffect, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'

import { Icon } from '../components/Icon'
import {
  createContent, createFolder, deleteContent, deleteFolder,
  listContent, listFolders, updateContent, updateFolder,
} from './learningApi'
import type { ContentItem, ContentItemType, LearningFolder } from './learningApi'

type EditorState =
  | { kind: 'folder'; target: LearningFolder | null }
  | { kind: 'content'; target: ContentItem | null }
  | null

const typeLabels: Record<ContentItemType, string> = {
  ARTICLE: '文章',
  NOTE: '笔记',
  CHECKLIST: '清单',
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
    .format(new Date(value))
}

export function LearningPage() {
  const [folders, setFolders] = useState<LearningFolder[]>([])
  const [selectedFolderId, setSelectedFolderId] = useState<string | null>(null)
  const [items, setItems] = useState<ContentItem[]>([])
  const [selectedItemId, setSelectedItemId] = useState<string | null>(null)
  const [editor, setEditor] = useState<EditorState>(null)
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const selectedFolder = folders.find((folder) => folder.id === selectedFolderId) ?? null
  const selectedItem = items.find((item) => item.id === selectedItemId) ?? null

  const loadFolders = useCallback(async () => {
    try {
      const loaded = await listFolders()
      setFolders(loaded)
      setSelectedFolderId((current) => loaded.some((folder) => folder.id === current) ? current : loaded[0]?.id ?? null)
      setError(null)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '学习库加载失败。')
    } finally {
      setIsLoading(false)
    }
  }, [])

  const loadItems = useCallback(async (folderId: string) => {
    try {
      const loaded = await listContent(folderId)
      setItems(loaded)
      setSelectedItemId((current) => loaded.some((item) => item.id === current) ? current : loaded[0]?.id ?? null)
      setError(null)
    } catch (cause) {
      setItems([])
      setSelectedItemId(null)
      setError(cause instanceof Error ? cause.message : '文档加载失败。')
    }
  }, [])

  useEffect(() => { void loadFolders() }, [loadFolders])
  useEffect(() => {
    if (selectedFolderId) void loadItems(selectedFolderId)
    else { setItems([]); setSelectedItemId(null) }
  }, [loadItems, selectedFolderId])

  async function handleSaveFolder(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const input = { name: String(form.get('name') ?? ''), description: String(form.get('description') ?? '') }
    setIsSaving(true)
    try {
      const saved = editor?.kind === 'folder' && editor.target
        ? await updateFolder(editor.target.id, input)
        : await createFolder(input)
      await loadFolders()
      setSelectedFolderId(saved.id)
      setEditor(null)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '文件夹保存失败。')
    } finally { setIsSaving(false) }
  }

  async function handleSaveContent(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedFolderId) return
    const form = new FormData(event.currentTarget)
    const input = {
      type: String(form.get('type')) as ContentItemType,
      title: String(form.get('title') ?? ''),
      body: String(form.get('body') ?? ''),
    }
    setIsSaving(true)
    try {
      const saved = editor?.kind === 'content' && editor.target
        ? await updateContent(editor.target.id, input)
        : await createContent(selectedFolderId, input)
      await loadItems(selectedFolderId)
      setSelectedItemId(saved.id)
      setEditor(null)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '文档保存失败。')
    } finally { setIsSaving(false) }
  }

  async function handleDeleteFolder(folderId: string) {
    if (deleteTarget !== `folder:${folderId}`) { setDeleteTarget(`folder:${folderId}`); return }
    setIsSaving(true)
    try {
      await deleteFolder(folderId)
      setDeleteTarget(null)
      setEditor(null)
      await loadFolders()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '文件夹删除失败。')
    } finally { setIsSaving(false) }
  }

  async function handleDeleteContent(contentId: string) {
    if (deleteTarget !== `content:${contentId}`) { setDeleteTarget(`content:${contentId}`); return }
    if (!selectedFolderId) return
    setIsSaving(true)
    try {
      await deleteContent(contentId)
      setDeleteTarget(null)
      setEditor(null)
      await loadItems(selectedFolderId)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '文档删除失败。')
    } finally { setIsSaving(false) }
  }

  return (
    <section className="learning-view">
      <aside className="learning-sidebar">
        <div className="section-label">
          <span>学习库</span>
          <button aria-label="创建文件夹" onClick={() => setEditor({ kind: 'folder', target: null })}><Icon name="plus" size={15} /></button>
        </div>
        <div className="folder-list">
          {folders.map((folder) => (
            <button className={selectedFolderId === folder.id ? 'active' : ''} key={folder.id} onClick={() => { setSelectedFolderId(folder.id); setEditor(null) }}>
              <Icon name="chevron-right" size={14} />
              <span className="folder-icon"><Icon name="folder" size={16} /></span>
              <strong>{folder.name}</strong>
              <small>{selectedFolderId === folder.id ? items.length : ''}</small>
            </button>
          ))}
        </div>
        {!isLoading && folders.length === 0 && (
          <button className="learning-sidebar-empty" onClick={() => setEditor({ kind: 'folder', target: null })}>
            <Icon name="folder" size={18} /><span>创建第一个文件夹</span>
          </button>
        )}
      </aside>

      <div className="learning-main">
        {error && <div className="page-error"><Icon name="alert-circle" size={16} /><span>{error}</span><button onClick={() => { setError(null); void loadFolders() }}>重试</button></div>}
        {isLoading ? (
          <div className="page-state">正在读取学习库…</div>
        ) : editor?.kind === 'folder' ? (
          <EditorShell title={editor.target ? '编辑文件夹' : '新建文件夹'} onClose={() => setEditor(null)}>
            <form className="resource-form" onSubmit={handleSaveFolder}>
              <label>文件夹名称<input autoFocus defaultValue={editor.target?.name} maxLength={160} name="name" required /></label>
              <label>用途说明<textarea defaultValue={editor.target?.description} maxLength={1000} name="description" rows={4} placeholder="这个文件夹准备沉淀什么内容？" /></label>
              <div className="resource-form-actions">
                {editor.target && <button className="danger-quiet" disabled={isSaving} onClick={() => void handleDeleteFolder(editor.target!.id)} type="button"><Icon name="trash" size={15} />{deleteTarget === `folder:${editor.target.id}` ? '再次点击确认删除' : '删除文件夹'}</button>}
                <span />
                <button className="secondary-button" onClick={() => setEditor(null)} type="button">取消</button>
                <button className="primary-button" disabled={isSaving} type="submit"><Icon name="save" size={15} />{isSaving ? '保存中…' : '保存'}</button>
              </div>
            </form>
          </EditorShell>
        ) : !selectedFolder ? (
          <div className="learning-welcome">
            <span><Icon name="book" size={22} /></span>
            <h1>建立你的学习空间</h1>
            <p>先创建一个主题文件夹，再把聊天结论、文章、笔记和行动清单沉淀进来。</p>
            <button className="primary-button" onClick={() => setEditor({ kind: 'folder', target: null })}><Icon name="plus" size={16} />创建文件夹</button>
          </div>
        ) : editor?.kind === 'content' ? (
          <EditorShell title={editor.target ? '编辑文档' : '新建文档'} onClose={() => setEditor(null)}>
            <form className="resource-form" onSubmit={handleSaveContent}>
              <label>内容类型<select defaultValue={editor.target?.type ?? 'ARTICLE'} name="type"><option value="ARTICLE">文章</option><option value="NOTE">笔记</option><option value="CHECKLIST">行动清单</option></select></label>
              <label>标题<input autoFocus defaultValue={editor.target?.title} maxLength={240} name="title" required /></label>
              <label>正文<textarea className="document-textarea" defaultValue={editor.target?.body} maxLength={20000} name="body" rows={14} required /></label>
              <div className="resource-form-actions">
                {editor.target && <button className="danger-quiet" disabled={isSaving} onClick={() => void handleDeleteContent(editor.target!.id)} type="button"><Icon name="trash" size={15} />{deleteTarget === `content:${editor.target.id}` ? '再次点击确认删除' : '删除文档'}</button>}
                <span />
                <button className="secondary-button" onClick={() => setEditor(null)} type="button">取消</button>
                <button className="primary-button" disabled={isSaving} type="submit"><Icon name="save" size={15} />{isSaving ? '保存中…' : '保存'}</button>
              </div>
            </form>
          </EditorShell>
        ) : (
          <div className="learning-content">
            <div className="breadcrumb"><span>学习库</span><Icon name="chevron-right" size={12} /><strong>{selectedFolder.name}</strong></div>
            <header className="learning-title-block resource-heading">
              <div><span className="type-badge"><Icon name="folder" size={13} />学习文件夹</span><small>{items.length} 篇内容</small></div>
              <div className="resource-title-row">
                <span><h1>{selectedFolder.name}</h1><p>{selectedFolder.description || '用于沉淀可复习、可继续编辑的学习内容。'}</p></span>
                <span className="resource-actions">
                  <button aria-label="编辑文件夹" onClick={() => setEditor({ kind: 'folder', target: selectedFolder })}><Icon name="pencil" size={16} /></button>
                  <button className="primary-icon-button" aria-label="新建文档" onClick={() => setEditor({ kind: 'content', target: null })}><Icon name="plus" size={17} /></button>
                </span>
              </div>
            </header>

            {items.length === 0 ? (
              <div className="content-empty">
                <span><Icon name="file" size={21} /></span>
                <strong>这个文件夹还是空的</strong>
                <p>创建文章、笔记或行动清单，内容会通过 API 保存并可在刷新后恢复。</p>
                <button className="primary-button" onClick={() => setEditor({ kind: 'content', target: null })}><Icon name="plus" size={15} />新建文档</button>
              </div>
            ) : (
              <div className="learning-document-layout">
                <div className="document-list">
                  {items.map((item) => (
                    <button className={selectedItemId === item.id ? 'active' : ''} key={item.id} onClick={() => setSelectedItemId(item.id)}>
                      <span className="document-icon"><Icon name={item.type === 'CHECKLIST' ? 'list' : 'file'} size={16} /></span>
                      <span><small>{typeLabels[item.type]} · {formatDate(item.updatedAt)}</small><strong>{item.title}</strong></span>
                      <Icon name="chevron-right" size={15} />
                    </button>
                  ))}
                </div>
                {selectedItem && (
                  <article className="document-reader">
                    <header>
                      <div><span>{typeLabels[selectedItem.type]}</span><small>更新于 {formatDate(selectedItem.updatedAt)}</small></div>
                      <button aria-label="编辑文档" onClick={() => setEditor({ kind: 'content', target: selectedItem })}><Icon name="pencil" size={15} />编辑</button>
                    </header>
                    <h2>{selectedItem.title}</h2>
                    <div className="document-body">{selectedItem.body}</div>
                  </article>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  )
}

function EditorShell({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  return (
    <div className="editor-shell">
      <header><div><span>学习库</span><h1>{title}</h1></div><button aria-label="关闭" onClick={onClose}><Icon name="x" size={18} /></button></header>
      {children}
    </div>
  )
}
