import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'

import { Icon } from '../components/Icon'
import {
  createContent, createFolder, deleteContent, deleteFolder,
  listContent, listFolders, updateContent, updateFolder,
} from './learningApi'
import type { ContentItem, ContentItemType, LearningFolder } from './learningApi'
import { LearningContentViewer, contentTypeConfig, contentTypeOrder } from './LearningContentViews'

type EditorState =
  | { kind: 'folder'; target: LearningFolder | null }
  | { kind: 'content'; target: ContentItem | null }
  | null

type MobileLevel = 'folders' | 'files' | 'content'

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
    .format(new Date(value))
}

export function LearningPage() {
  const [folders, setFolders] = useState<LearningFolder[]>([])
  const [selectedFolderId, setSelectedFolderId] = useState<string | null>(null)
  const [items, setItems] = useState<ContentItem[]>([])
  const [selectedItemId, setSelectedItemId] = useState<string | null>(null)
  const [mobileLevel, setMobileLevel] = useState<MobileLevel>('folders')
  const [editor, setEditor] = useState<EditorState>(null)
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const selectedFolder = folders.find((folder) => folder.id === selectedFolderId) ?? null
  const selectedItem = items.find((item) => item.id === selectedItemId) ?? null
  const groupedItems = useMemo(() => contentTypeOrder
    .map((type) => ({ type, items: items.filter((item) => item.type === type) }))
    .filter((group) => group.items.length > 0), [items])

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

  function handleSelectFolder(folderId: string) {
    setSelectedFolderId(folderId)
    setSelectedItemId(null)
    setEditor(null)
    setDeleteTarget(null)
    setMobileLevel('files')
  }

  function handleSelectItem(itemId: string) {
    setSelectedItemId(itemId)
    setEditor(null)
    setDeleteTarget(null)
    setMobileLevel('content')
  }

  function handleOpenEditor(nextEditor: Exclude<EditorState, null>) {
    setEditor(nextEditor)
    setDeleteTarget(null)
    setMobileLevel('content')
  }

  function handleCloseEditor() {
    const closingKind = editor?.kind
    setEditor(null)
    setDeleteTarget(null)
    setMobileLevel(closingKind === 'folder' ? 'folders' : selectedItem ? 'content' : 'files')
  }

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
      setMobileLevel('files')
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
      setMobileLevel('content')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '内容保存失败。')
    } finally { setIsSaving(false) }
  }

  async function handleDeleteFolder(folderId: string) {
    if (deleteTarget !== `folder:${folderId}`) { setDeleteTarget(`folder:${folderId}`); return }
    setIsSaving(true)
    try {
      await deleteFolder(folderId)
      setDeleteTarget(null)
      setEditor(null)
      setMobileLevel('folders')
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
      setMobileLevel('files')
      await loadItems(selectedFolderId)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '内容删除失败。')
    } finally { setIsSaving(false) }
  }

  return (
    <section className={`learning-view mobile-${mobileLevel}`}>
      <aside className="learning-pane folder-pane">
        <header className="pane-header">
          <div><small>学习</small><strong>学习库</strong></div>
          <button aria-label="创建文件夹" className="icon-button" onClick={() => handleOpenEditor({ kind: 'folder', target: null })}><Icon name="plus" size={18} /></button>
        </header>
        <div className="folder-list">
          {folders.map((folder) => (
            <div className={selectedFolderId === folder.id ? 'folder-row active' : 'folder-row'} key={folder.id}>
              <button className="folder-select" onClick={() => handleSelectFolder(folder.id)}>
                <span className="folder-icon"><Icon name="folder" size={17} /></span>
                <span><strong>{folder.name}</strong><small>{folder.description || '未填写说明'}</small></span>
                <Icon name="chevron-right" size={16} />
              </button>
            </div>
          ))}
        </div>
        {!isLoading && folders.length === 0 && (
          <button className="pane-empty-action" onClick={() => handleOpenEditor({ kind: 'folder', target: null })}>
            <Icon name="folder" size={20} /><strong>创建第一个文件夹</strong><span>按主题整理长期学习内容</span>
          </button>
        )}
      </aside>

      <section className="learning-pane file-pane">
        <MobileBack label="学习库" onClick={() => setMobileLevel('folders')} />
        <header className="pane-header file-pane-header">
          <div><small>文件夹</small><strong>{selectedFolder?.name ?? '选择文件夹'}</strong></div>
          {selectedFolder && <button aria-label="新建内容" className="icon-button primary" onClick={() => handleOpenEditor({ kind: 'content', target: null })}><Icon name="plus" size={18} /></button>}
        </header>

        {selectedFolder && (
          <div className="folder-summary">
            <p>{selectedFolder.description || '用于沉淀可复习、可继续编辑的学习内容。'}</p>
            <button onClick={() => handleOpenEditor({ kind: 'folder', target: selectedFolder })}><Icon name="pencil" size={14} />编辑文件夹</button>
          </div>
        )}

        <div className="grouped-file-list">
          {groupedItems.map((group) => (
            <section key={group.type}>
              <h2>{contentTypeConfig[group.type].group}</h2>
              <div className="file-group-card">
                {group.items.map((item) => {
                  const config = contentTypeConfig[item.type]
                  return (
                    <button className={selectedItemId === item.id ? 'file-row active' : 'file-row'} key={item.id} onClick={() => handleSelectItem(item.id)}>
                      <span className={`file-type-icon ${config.tone}`}><Icon name={config.icon} size={17} /></span>
                      <span><strong>{item.title}</strong><small>{config.label} · {formatDate(item.updatedAt)}</small></span>
                      <Icon name="chevron-right" size={16} />
                    </button>
                  )
                })}
              </div>
            </section>
          ))}
        </div>

        {!isLoading && selectedFolder && items.length === 0 && (
          <button className="pane-empty-action compact" onClick={() => handleOpenEditor({ kind: 'content', target: null })}>
            <Icon name="file" size={20} /><strong>新建第一份内容</strong><span>支持路径、文章、测验与清单</span>
          </button>
        )}
      </section>

      <main className="learning-pane content-pane">
        {error && <div className="page-error"><Icon name="alert-circle" size={17} /><span>{error}</span><button onClick={() => { setError(null); void loadFolders() }}>重试</button></div>}
        {isLoading ? (
          <div className="page-state">正在读取学习库…</div>
        ) : editor?.kind === 'folder' ? (
          <EditorShell backLabel="学习库" title={editor.target ? '编辑文件夹' : '新建文件夹'} onClose={handleCloseEditor}>
            <form className="resource-form" onSubmit={handleSaveFolder}>
              <label>文件夹名称<input autoFocus defaultValue={editor.target?.name} maxLength={160} name="name" required /></label>
              <label>用途说明<textarea defaultValue={editor.target?.description} maxLength={1000} name="description" rows={4} placeholder="这个文件夹准备沉淀什么内容？" /></label>
              <div className="resource-form-actions">
                {editor.target && <button className="danger-quiet" disabled={isSaving} onClick={() => void handleDeleteFolder(editor.target!.id)} type="button"><Icon name="trash" size={16} />{deleteTarget === `folder:${editor.target.id}` ? '再次点击确认删除' : '删除文件夹'}</button>}
                <span />
                <button className="secondary-button" onClick={handleCloseEditor} type="button">取消</button>
                <button className="primary-button" disabled={isSaving} type="submit"><Icon name="save" size={16} />{isSaving ? '保存中…' : '保存'}</button>
              </div>
            </form>
          </EditorShell>
        ) : editor?.kind === 'content' ? (
          <EditorShell backLabel={selectedFolder?.name ?? '文件列表'} title={editor.target ? '编辑内容' : '新建内容'} onClose={handleCloseEditor}>
            <ContentEditorForm
              key={editor.target?.id ?? 'new-content'}
              isSaving={isSaving}
              onCancel={handleCloseEditor}
              onDelete={editor.target ? () => void handleDeleteContent(editor.target!.id) : undefined}
              onSubmit={handleSaveContent}
              target={editor.target}
              deleteLabel={editor.target && deleteTarget === `content:${editor.target.id}` ? '再次点击确认删除' : '删除内容'}
            />
          </EditorShell>
        ) : selectedItem ? (
          <>
            <MobileBack label={selectedFolder?.name ?? '文件列表'} onClick={() => setMobileLevel('files')} />
            <LearningContentViewer item={selectedItem} onEdit={() => handleOpenEditor({ kind: 'content', target: selectedItem })} />
          </>
        ) : (
          <div className="content-placeholder">
            <span><Icon name="book" size={24} /></span>
            <strong>{selectedFolder ? '选择一份内容开始阅读' : '从左侧选择学习文件夹'}</strong>
            <p>学习库采用“文件夹 → 文件 → 内容”三级结构，减少寻找入口的成本。</p>
          </div>
        )}
      </main>
    </section>
  )
}

function ContentEditorForm({
  target, isSaving, deleteLabel, onSubmit, onCancel, onDelete,
}: {
  target: ContentItem | null
  isSaving: boolean
  deleteLabel: string
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onCancel: () => void
  onDelete?: () => void
}) {
  const [mode, setMode] = useState<'edit' | 'preview'>('edit')
  const [type, setType] = useState<ContentItemType>(target?.type ?? 'ARTICLE')
  const [title, setTitle] = useState(target?.title ?? '')
  const [body, setBody] = useState(target?.body ?? '')
  const previewItem: ContentItem = {
    id: target?.id ?? 'preview',
    folderId: target?.folderId ?? 'preview',
    sourceSkillRunId: target?.sourceSkillRunId ?? null,
    type,
    title: title.trim() || '无标题内容',
    body: body.trim() || '开始输入正文后，这里会显示最终阅读效果。',
    verificationStatus: target?.verificationStatus ?? 'USER_AUTHORED',
    createdAt: target?.createdAt ?? new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }

  return <form className="resource-form document-editor" onSubmit={onSubmit}>
    <input name="type" type="hidden" value={type} />
    <input name="title" type="hidden" value={title} />
    <input name="body" type="hidden" value={body} />
    <div className="editor-mode-control" role="tablist" aria-label="内容编辑模式">
      <button aria-selected={mode === 'edit'} className={mode === 'edit' ? 'active' : ''} onClick={() => setMode('edit')} role="tab" type="button">编辑</button>
      <button aria-selected={mode === 'preview'} className={mode === 'preview' ? 'active' : ''} onClick={() => setMode('preview')} role="tab" type="button">预览</button>
    </div>
    {mode === 'edit' ? <div className="document-fields">
      <label>内容类型<select value={type} onChange={(event) => setType(event.target.value as ContentItemType)}><option value="LEARNING_PATH">学习路径</option><option value="ARTICLE">文章</option><option value="NOTE">笔记</option><option value="QUIZ">测验</option><option value="CHECKLIST">行动清单</option></select></label>
      <label>标题<input autoFocus maxLength={240} onChange={(event) => setTitle(event.target.value)} required value={title} /></label>
      <label>正文<textarea className="document-textarea" maxLength={20000} onChange={(event) => setBody(event.target.value)} required rows={14} value={body} /></label>
      <p className="editor-help">文章支持 ## 二级标题、``` 代码块和独占一行的 ![说明](https://图片地址)；路径可用 [x] 标记完成；测验使用“题目、- 选项、答案: 1”，多题用 --- 分隔。</p>
    </div> : <div className="document-preview"><LearningContentViewer item={previewItem} /></div>}
    <div className="resource-form-actions">
      {onDelete && <button className="danger-quiet" disabled={isSaving} onClick={onDelete} type="button"><Icon name="trash" size={16} />{deleteLabel}</button>}
      <span />
      <button className="secondary-button" onClick={onCancel} type="button">取消</button>
      <button className="primary-button" disabled={isSaving || !title.trim() || !body.trim()} type="submit"><Icon name="save" size={16} />{isSaving ? '保存中…' : '保存并返回阅读'}</button>
    </div>
  </form>
}

function MobileBack({ label, onClick }: { label: string; onClick: () => void }) {
  return <button className="mobile-back" onClick={onClick}><Icon name="arrow-left" size={17} />{label}</button>
}

function EditorShell({ backLabel, title, onClose, children }: { backLabel: string; title: string; onClose: () => void; children: ReactNode }) {
  return (
    <div className="editor-shell">
      <button className="editor-back" onClick={onClose}><Icon name="arrow-left" size={17} />{backLabel}</button>
      <header><div><small>学习库</small><h1>{title}</h1></div><button aria-label="关闭" className="icon-button" onClick={onClose}><Icon name="x" size={18} /></button></header>
      {children}
    </div>
  )
}
