import {
  Activity,
  ArrowRight,
  BookOpen,
  Check,
  Compass,
  MessageSquare,
  Plus,
  Rss,
  Send,
  Sparkles,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

export type IconName =
  | 'activity'
  | 'arrow-right'
  | 'book'
  | 'check'
  | 'compass'
  | 'message'
  | 'plus'
  | 'rss'
  | 'send'
  | 'sparkles'

type IconProps = {
  name: IconName
  size?: number
  strokeWidth?: number
}

const icons: Record<IconName, LucideIcon> = {
  activity: Activity,
  'arrow-right': ArrowRight,
  book: BookOpen,
  check: Check,
  compass: Compass,
  message: MessageSquare,
  plus: Plus,
  rss: Rss,
  send: Send,
  sparkles: Sparkles,
}

export function Icon({ name, size = 20, strokeWidth = 1.7 }: IconProps) {
  const IconComponent = icons[name]
  return <IconComponent aria-hidden="true" className="icon" size={size} strokeWidth={strokeWidth} />
}
