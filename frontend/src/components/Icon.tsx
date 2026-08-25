import {
  Activity,
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
  Plus,
  RefreshCw,
  Rss,
  Search,
  Send,
  Settings,
  ShieldCheck,
  Sparkles,
  Target,
  Zap,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

export type IconName =
  | 'activity'
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
  | 'plus'
  | 'refresh'
  | 'rss'
  | 'search'
  | 'send'
  | 'settings'
  | 'shield'
  | 'sparkles'
  | 'target'
  | 'zap'

type IconProps = {
  name: IconName
  size?: number
  strokeWidth?: number
}

const icons: Record<IconName, LucideIcon> = {
  activity: Activity,
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
  plus: Plus,
  refresh: RefreshCw,
  rss: Rss,
  search: Search,
  send: Send,
  settings: Settings,
  shield: ShieldCheck,
  sparkles: Sparkles,
  target: Target,
  zap: Zap,
}

export function Icon({ name, size = 18, strokeWidth = 1.9 }: IconProps) {
  const IconComponent = icons[name]
  return <IconComponent aria-hidden="true" className="icon" size={size} strokeWidth={strokeWidth} />
}
