import {
  Activity,
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  BookOpen,
  Bookmark,
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Clock3,
  Code2,
  ExternalLink,
  FileText,
  Filter,
  Folder,
  ListChecks,
  MessageSquare,
  Pencil,
  Plus,
  RefreshCw,
  Rss,
  Search,
  Send,
  Save,
  Settings,
  ShieldCheck,
  Sparkles,
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
  | 'bookmark'
  | 'check'
  | 'check-circle'
  | 'chevron-down'
  | 'chevron-right'
  | 'clock'
  | 'code'
  | 'external-link'
  | 'file'
  | 'filter'
  | 'folder'
  | 'list'
  | 'message'
  | 'pencil'
  | 'plus'
  | 'refresh'
  | 'rss'
  | 'search'
  | 'send'
  | 'save'
  | 'settings'
  | 'shield'
  | 'sparkles'
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
  bookmark: Bookmark,
  check: Check,
  'check-circle': CheckCircle2,
  'chevron-down': ChevronDown,
  'chevron-right': ChevronRight,
  clock: Clock3,
  code: Code2,
  'external-link': ExternalLink,
  file: FileText,
  filter: Filter,
  folder: Folder,
  list: ListChecks,
  message: MessageSquare,
  pencil: Pencil,
  plus: Plus,
  refresh: RefreshCw,
  rss: Rss,
  search: Search,
  send: Send,
  save: Save,
  settings: Settings,
  shield: ShieldCheck,
  sparkles: Sparkles,
  target: Target,
  trash: Trash2,
  x: X,
  zap: Zap,
}

export function Icon({ name, size = 18, strokeWidth = 1.9 }: IconProps) {
  const IconComponent = icons[name]
  return <IconComponent aria-hidden="true" className="icon" size={size} strokeWidth={strokeWidth} />
}
