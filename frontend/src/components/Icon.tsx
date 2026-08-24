type IconName =
  | 'activity'
  | 'arrow-right'
  | 'arrow-up'
  | 'book'
  | 'check'
  | 'compass'
  | 'message'
  | 'plus'
  | 'sparkles'

type IconProps = {
  name: IconName
  size?: number
  strokeWidth?: number
}

const paths: Record<IconName, ReactNode> = {
  activity: <><path d="M3 12h3l2.2-6 4.1 12 2.2-6H21" /></>,
  'arrow-right': <><path d="M5 12h14" /><path d="m14 7 5 5-5 5" /></>,
  'arrow-up': <><path d="m12 19 0-14" /><path d="m7 10 5-5 5 5" /></>,
  book: <><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H11v16H6.5A2.5 2.5 0 0 0 4 21.5Z" /><path d="M20 5.5A2.5 2.5 0 0 0 17.5 3H13v16h4.5a2.5 2.5 0 0 1 2.5 2.5Z" /></>,
  check: <><path d="m5 12 4 4L19 6" /></>,
  compass: <><circle cx="12" cy="12" r="9" /><path d="m15.5 8.5-2 5-5 2 2-5Z" /></>,
  message: <><path d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4Z" /></>,
  plus: <><path d="M12 5v14M5 12h14" /></>,
  sparkles: <><path d="m12 3 1.1 3.1L16 7.5l-2.9 1.4L12 12l-1.1-3.1L8 7.5l2.9-1.4Z" /><path d="m18.5 14 .7 1.8 1.8.7-1.8.7-.7 1.8-.7-1.8-1.8-.7 1.8-.7Z" /><path d="m5.5 13 .8 2.2 2.2.8-2.2.8L5.5 19l-.8-2.2-2.2-.8 2.2-.8Z" /></>,
}

export function Icon({ name, size = 20, strokeWidth = 1.8 }: IconProps) {
  return (
    <svg
      aria-hidden="true"
      className="icon"
      fill="none"
      height={size}
      viewBox="0 0 24 24"
      width={size}
    >
      <g stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth={strokeWidth}>
        {paths[name]}
      </g>
    </svg>
  )
}
import type { ReactNode } from 'react'
