import {
  Activity,
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  BookOpen,
  BrainCircuit,
  Bookmark,
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Clock3,
  Code2,
  Copy,
  ExternalLink,
  FileText,
  FileQuestion,
  Filter,
  Folder,
  Globe2,
  Highlighter,
  History,
  LibraryBig,
  ListChecks,
  Moon,
  MessageSquare,
  MessageCircle,
  Pencil,
  Plus,
  RefreshCw,
  Rss,
  Route,
  Search,
  Send,
  Save,
  Settings,
  ShieldCheck,
  Sparkles,
  Sun,
  StickyNote,
  Target,
  Trash2,
  X,
  Zap,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

export type IconName =
  | 'activity'
  | 'alert-circle'
  | 'arrow-left'
  | 'arrow-right'
  | 'book'
  | 'brain'
  | 'bookmark'
  | 'check'
  | 'check-circle'
  | 'chevron-down'
  | 'chevron-right'
  | 'clock'
  | 'code'
  | 'copy'
  | 'external-link'
  | 'file'
  | 'file-question'
  | 'filter'
  | 'folder'
  | 'globe'
  | 'highlight'
  | 'history'
  | 'library'
  | 'list'
  | 'moon'
  | 'message'
  | 'message-circle'
  | 'pencil'
  | 'plus'
  | 'refresh'
  | 'rss'
  | 'route'
  | 'search'
  | 'send'
  | 'save'
  | 'settings'
  | 'shield'
  | 'sparkles'
  | 'sun'
  | 'note'
  | 'target'
  | 'trash'
  | 'x'
  | 'zap'

type IconProps = {
  name: IconName
  size?: number
  strokeWidth?: number
}

const icons: Record<IconName, LucideIcon> = {
  activity: Activity,
  'alert-circle': AlertCircle,
  'arrow-left': ArrowLeft,
  'arrow-right': ArrowRight,
  book: BookOpen,
  brain: BrainCircuit,
  bookmark: Bookmark,
  check: Check,
  'check-circle': CheckCircle2,
  'chevron-down': ChevronDown,
  'chevron-right': ChevronRight,
  clock: Clock3,
  code: Code2,
  copy: Copy,
  'external-link': ExternalLink,
  file: FileText,
  'file-question': FileQuestion,
  filter: Filter,
  folder: Folder,
  globe: Globe2,
  highlight: Highlighter,
  history: History,
  library: LibraryBig,
  list: ListChecks,
  moon: Moon,
  message: MessageSquare,
  'message-circle': MessageCircle,
  pencil: Pencil,
  plus: Plus,
  refresh: RefreshCw,
  rss: Rss,
  route: Route,
  search: Search,
  send: Send,
  save: Save,
  settings: Settings,
  shield: ShieldCheck,
  sparkles: Sparkles,
  sun: Sun,
  note: StickyNote,
  target: Target,
  trash: Trash2,
  x: X,
  zap: Zap,
}

export function Icon({ name, size = 18, strokeWidth = 1.9 }: IconProps) {
  const IconComponent = icons[name]
  return <IconComponent aria-hidden="true" className="icon" size={size} strokeWidth={strokeWidth} />
}
