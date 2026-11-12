import SearchIcon from '@mui/icons-material/Search'
import IconButton from '@mui/material/IconButton'
import InputBase from '@mui/material/InputBase'
import Paper from '@mui/material/Paper'
import { useState, type SubmitEventHandler } from 'react'

export const MIN_SEARCH_LENGTH = 3

interface SearchBarProps {
  readonly onSearch?: (query: string) => void
}

export const SearchBar = ({ onSearch }: SearchBarProps) => {
  const [query, setQuery] = useState('')
  const trimmed = query.trim()
  const isTooShort = trimmed.length > 0 && trimmed.length < MIN_SEARCH_LENGTH

  const handleSubmit: SubmitEventHandler<HTMLFormElement> = (event) => {
    event.preventDefault()
    if (!isTooShort) onSearch?.(trimmed)
  }

  return (
    <Paper
      component="form"
      role="search"
      variant="outlined"
      onSubmit={handleSubmit}
      sx={{ display: 'flex', alignItems: 'center', px: 0.5, width: '100%', maxWidth: 560, borderRadius: 999 }}
    >
      <IconButton
        type="submit"
        size="small"
        aria-label="Search"
        title={isTooShort ? `Type at least ${MIN_SEARCH_LENGTH} characters` : 'Search'}
        disabled={isTooShort}
      >
        <SearchIcon />
      </IconButton>
      <InputBase
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Search products..."
        inputProps={{ 'aria-label': 'Search products', 'aria-invalid': isTooShort }}
        sx={{ ml: 0.5, flex: 1, py: 0.5 }}
      />
    </Paper>
  )
}
