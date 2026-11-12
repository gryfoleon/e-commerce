import AddShoppingCartIcon from '@mui/icons-material/AddShoppingCart'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlined'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import InventoryIcon from '@mui/icons-material/Inventory2Outlined'
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined'
import Box from '@mui/material/Box'
import IconButton from '@mui/material/IconButton'
import Paper from '@mui/material/Paper'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TablePagination from '@mui/material/TablePagination'
import TableRow from '@mui/material/TableRow'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import { Loader } from '../../../shared/components/Loader'
import { truncate } from '../util/product.utils'
import type { Product } from '../types/product.types'

const PAGE_SIZE_OPTIONS = [10, 15, 25, 50]

interface ProductTableProps {
  readonly products: readonly Product[]
  readonly total: number
  readonly page: number
  readonly pageSize: number
  readonly isLoading?: boolean
  readonly onPageChange: (page: number) => void
  readonly onPageSizeChange: (pageSize: number) => void
  readonly onView?: (product: Product) => void
  readonly onEdit?: (product: Product) => void
  readonly onDelete?: (product: Product) => void
  readonly onAddToCart?: (product: Product) => void
  readonly canAddToCart?: (product: Product) => boolean
}

const HEADERS: ReadonlyArray<{ label: string; align: 'left' | 'right' | 'center' }> = [
  { label: 'SKU', align: 'left' },
  { label: 'Name', align: 'left' },
  { label: 'Description', align: 'left' },
  { label: 'Category', align: 'left' },
  { label: 'Price', align: 'right' },
  { label: 'Stock', align: 'center' },
  { label: 'Weight', align: 'right' },
  { label: 'Actions', align: 'center' }
]

const stockColor = (stock: number) => {
  if (stock === 0) return { bg: 'error.light', fg: 'error.dark' }
  if (stock <= 5) return { bg: 'warning.light', fg: 'warning.dark' }
  return { bg: 'success.light', fg: 'success.dark' }
}

export const ProductTable = ({
  products,
  total,
  page,
  pageSize,
  isLoading = false,
  onPageChange,
  onPageSizeChange,
  onView,
  onEdit,
  onDelete,
  onAddToCart,
  canAddToCart = (product) => product.stock > 0
}: ProductTableProps) => (
  <TableContainer component={Paper} variant='outlined' sx={{ position: 'relative' }}>
    {isLoading && <Loader overlay label='Loading products' />}
    <Table aria-label='products table' aria-busy={isLoading}>
      <TableHead>
        <TableRow>
          {HEADERS.map((h) => (
            <TableCell key={h.label} align={h.align}>
              {h.label}
            </TableCell>
          ))}
        </TableRow>
      </TableHead>
      <TableBody>
        {isLoading && products.length === 0 && (
          <TableRow>
            <TableCell colSpan={HEADERS.length} sx={{ height: 200 }} />
          </TableRow>
        )}
        {!isLoading && products.length === 0 && (
          <TableRow>
            <TableCell colSpan={HEADERS.length}>
              <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 6, color: 'text.secondary' }}>
                <InventoryIcon fontSize='large' />
                <Typography variant='subtitle1' sx={{ fontWeight: 600 }}>
                  No products found
                </Typography>
                <Typography variant='body2'>Try adjusting the filters or add a new product.</Typography>
              </Box>
            </TableCell>
          </TableRow>
        )}
        {products.map((product) => {
            const color = stockColor(product.stock)
            return (
              <TableRow key={product.id} hover>
                <TableCell>{product.sku}</TableCell>
                <TableCell sx={{ fontWeight: 500 }}>{product.name}</TableCell>
                <TableCell sx={{ color: 'text.secondary' }}>{truncate(product.description, 50)}</TableCell>
                <TableCell>{product.category}</TableCell>
                <TableCell align='right'>${product.price.toFixed(2)}</TableCell>
                <TableCell align='center'>
                  <Box component='span' sx={{ px: 1, py: 0.25, borderRadius: 1, fontSize: 12, fontWeight: 600, bgcolor: color.bg, color: color.fg }}>
                    {product.stock} un.
                  </Box>
                </TableCell>
                <TableCell align='right'>{product.weightKg === null ? '—' : `${product.weightKg.toFixed(2)} kg`}</TableCell>
                <TableCell align='center' sx={{ whiteSpace: 'nowrap' }}>
                  {onAddToCart && (
                    <Tooltip title={canAddToCart(product) ? 'Add to cart' : 'No more stock available'}>
                      <span>
                        <IconButton
                          size='small'
                          color='primary'
                          aria-label={`Add ${product.name} to cart`}
                          disabled={!canAddToCart(product)}
                          onClick={() => onAddToCart(product)}
                        >
                          <AddShoppingCartIcon />
                        </IconButton>
                      </span>
                    </Tooltip>
                  )}
                  <Tooltip title='View detail'>
                    <IconButton size='small' aria-label={`View detail of ${product.name}`} onClick={() => onView?.(product)}>
                      <VisibilityOutlinedIcon />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title='Edit'>
                    <IconButton size='small' aria-label={`Edit ${product.name}`} onClick={() => onEdit?.(product)}>
                      <EditOutlinedIcon />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title='Delete'>
                    <IconButton size='small' aria-label={`Delete ${product.name}`} onClick={() => onDelete?.(product)}>
                      <DeleteOutlineIcon />
                    </IconButton>
                  </Tooltip>
                </TableCell>
              </TableRow>
            )
          })}
      </TableBody>
    </Table>
    <TablePagination
      component='div'
      count={total}
      page={page}
      rowsPerPage={pageSize}
      rowsPerPageOptions={PAGE_SIZE_OPTIONS}
      onPageChange={(_, p) => onPageChange(p)}
      onRowsPerPageChange={(e) => onPageSizeChange(Number(e.target.value))}
    />
  </TableContainer>
)
