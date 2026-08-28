import { useEffect, useState } from 'react'

export type ThemeMode = 'light' | 'dark'

const THEME_KEY = 'lifeskill.theme'

function initialTheme(): ThemeMode {
  const saved = localStorage.getItem(THEME_KEY)
  if (saved === 'light' || saved === 'dark') return saved
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function useTheme() {
  const [theme, setTheme] = useState<ThemeMode>(initialTheme)
  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.documentElement.style.colorScheme = theme
    localStorage.setItem(THEME_KEY, theme)
  }, [theme])
  return { theme, toggleTheme: () => setTheme((value) => value === 'dark' ? 'light' : 'dark') }
}
