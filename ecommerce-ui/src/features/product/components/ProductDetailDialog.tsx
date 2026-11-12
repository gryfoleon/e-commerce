import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Grid from '@mui/material/Grid'
import Typography from '@mui/material/Typography'
import type { Product } from '../types/product.types'

interface ProductDetailDialogProps {
  readonly product: Product | null
  readonly onClose: () => void
}

export const ProductDetailDialog = ({ product, onClose }: ProductDetailDialogProps) => {
  const rows: ReadonlyArray<[string, string]> = product
    ? [
        ['ID', product.id],
        ['SKU', product.sku],
        ['Name', product.name],
        ['Description', product.description ?? '—'],
        ['Category', product.category],
        ['Price', `$${product.price.toFixed(2)}`],
        ['Stock', `${product.stock} un.`],
        ['Weight', product.weightKg === null ? '—' : `${product.weightKg.toFixed(2)} kg`]
      ]
    : []

  return (
    <Dialog open={product !== null} onClose={onClose} fullWidth maxWidth='sm' aria-labelledby='product-detail-title'>
      <DialogTitle id='product-detail-title'>Product detail</DialogTitle>
      <DialogContent dividers>
        <Grid container spacing={2}>
          {rows.map(([label, value]) => (
            <Grid key={label} size={{ xs: 12, sm: label === 'Description' || label === 'ID' || label === 'Name' ? 12 : 6 }}>
              <Typography variant='caption' color='text.secondary'>
                {label}
              </Typography>
              <Typography sx={{ wordBreak: 'break-word' }}>{value}</Typography>
            </Grid>
          ))}
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  )
}
