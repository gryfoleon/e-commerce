import type { CategoryOption } from '../types/category.types'

export const withSelectedOption = (options: CategoryOption[], selected: string | null | undefined): CategoryOption[] =>
  selected && !options.some((option) => option.value === selected)
    ? [...options, { value: selected, label: selected }]
    : options
