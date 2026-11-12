import AddIcon from '@mui/icons-material/Add'
import CloseIcon from '@mui/icons-material/Close'
import FileUploadIcon from '@mui/icons-material/FileUpload'
import SearchIcon from '@mui/icons-material/Search'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useRef, type ChangeEvent } from 'react'
import { useCategoryOptionsQuery } from '../../category/hooks/category.query'
import { withSelectedOption } from '../../category/util/category.utils'
import { useProductSearchActions } from '../hooks/product-search'
import { useProductFilterStore } from '../stores/product-filter.store'
import type { StockState } from '../types/product.types'

interface ProductFilterBarProps {
  readonly onAddProduct: () => void
  readonly onImportCsv: (file: File) => void
}

const STOCK_OPTIONS: ReadonlyArray<{ value: StockState; label: string }> = [
  { value: 'any', label: 'Any' },
  { value: 'in-stock', label: 'In stock' },
  { value: 'low', label: 'Low stock' },
  { value: 'out', label: 'Out of stock' }
]

const Field = ({ label, children }: { label: string; children: React.ReactNode }) => (
  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
    <Typography variant='body2' component='label' sx={{ fontWeight: 600 }}>
      {label}
    </Typography>
    {children}
  </Box>
)

export const ProductFilterBar = ({ onAddProduct, onImportCsv }: ProductFilterBarProps) => {
  const fileInput = useRef<HTMLInputElement>(null)
  const { draft, setDraft, clearFilters } = useProductFilterStore()
  const { applyFilters } = useProductSearchActions()
  const { options: categoryOptions, isPending: categoriesLoading, isError: categoriesFailed } = useCategoryOptionsQuery()
  const categories = withSelectedOption(categoryOptions, draft.category)

  const handleFile = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (file) onImportCsv(file)
    event.target.value = ''
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'flex-end', gap: 1 }}>
        <input ref={fileInput} type='file' accept='.csv,text/csv' hidden onChange={handleFile} data-testid='csv-input' />
        <Button variant='contained' color='success' startIcon={<FileUploadIcon />} onClick={() => fileInput.current?.click()}>
          Import CSV
        </Button>
        <Button variant='contained' startIcon={<AddIcon />} onClick={onAddProduct}>
          Add product
        </Button>
      </Box>

      <Paper
        component='form'
        role='search'
        aria-label='Product filters'
        onSubmit={(e) => {
          e.preventDefault()
          applyFilters()
        }}
        sx={{ p: 2, display: 'flex', flexWrap: 'wrap', alignItems: 'flex-end', gap: 2 }}
      >
        <Field label='SKU'>
          <TextField
            placeholder='SKU'
            value={draft.sku}
            onChange={(e) => setDraft({ sku: e.target.value })}
            slotProps={{ htmlInput: { 'aria-label': 'SKU' } }}
          />
        </Field>
        <Field label='Category'>
          <TextField
            select
            value={draft.category}
            onChange={(e) => setDraft({ category: e.target.value })}
            sx={{ minWidth: 200 }}
            slotProps={{ select: { displayEmpty: true }, htmlInput: { 'aria-label': 'Category' } }}
          >
            <MenuItem value=''>All</MenuItem>
            {categories.map((o) => (
              <MenuItem key={o.value} value={o.value}>
                {o.label}
              </MenuItem>
            ))}
            {categoriesLoading && (
              <MenuItem disabled role='status'>
                Loading categories…
              </MenuItem>
            )}
            {categoriesFailed && (
              <MenuItem disabled role='alert'>
                Could not load categories
              </MenuItem>
            )}
          </TextField>
        </Field>
        <Field label='Price Range'>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <TextField
              type='number'
              placeholder='Min ($)'
              value={draft.priceMin}
              onChange={(e) => setDraft({ priceMin: e.target.value })}
              sx={{ width: 110 }}
              slotProps={{ htmlInput: { min: 0, 'aria-label': 'Minimum price' } }}
            />
            <TextField
              type='number'
              placeholder='Max ($)'
              value={draft.priceMax}
              onChange={(e) => setDraft({ priceMax: e.target.value })}
              sx={{ width: 110 }}
              slotProps={{ htmlInput: { min: 0, 'aria-label': 'Maximum price' } }}
            />
          </Box>
        </Field>
        <Field label='Weight Range'>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <TextField
              type='number'
              placeholder='Min (kg)'
              value={draft.weightMin}
              onChange={(e) => setDraft({ weightMin: e.target.value })}
              sx={{ width: 110 }}
              slotProps={{ htmlInput: { min: 0, 'aria-label': 'Minimum weight' } }}
            />
            <TextField
              type='number'
              placeholder='Max (kg)'
              value={draft.weightMax}
              onChange={(e) => setDraft({ weightMax: e.target.value })}
              sx={{ width: 110 }}
              slotProps={{ htmlInput: { min: 0, 'aria-label': 'Maximum weight' } }}
            />
          </Box>
        </Field>
        <Field label='Stock State'>
          <TextField
            select
            value={draft.stockState}
            onChange={(e) => setDraft({ stockState: e.target.value as StockState })}
            sx={{ minWidth: 170 }}
            slotProps={{ htmlInput: { 'aria-label': 'Stock state' } }}
          >
            {STOCK_OPTIONS.map((o) => (
              <MenuItem key={o.value} value={o.value}>
                {o.label}
              </MenuItem>
            ))}
          </TextField>
        </Field>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button type='submit' variant='contained' startIcon={<SearchIcon />} sx={{ height: 40 }}>
            Apply filters
          </Button>
          <Button type='button' variant='outlined' color='inherit' startIcon={<CloseIcon />} onClick={clearFilters} sx={{ height: 40 }}>
            Clear filters
          </Button>
        </Box>
      </Paper>
    </Box>
  )
}
