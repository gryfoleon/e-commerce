import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'
import type { Product } from '../types/product.types'

interface ProductDeleteDialogProps {
  readonly product: Product | null
  readonly isDeleting?: boolean
  readonly onClose: () => void
  readonly onConfirm: () => void
}

export const ProductDeleteDialog = ({ product, isDeleting, onClose, onConfirm }: ProductDeleteDialogProps) => (
  <Dialog
    open={product !== null}
    onClose={isDeleting ? undefined : onClose}
    fullWidth
    maxWidth='xs'
    aria-labelledby='product-delete-title'
  >
    <DialogTitle id='product-delete-title'>Delete product</DialogTitle>
    <DialogContent>
      <DialogContentText>
        Are you sure you want to delete <strong>{product?.name}</strong> ({product?.sku})? This action cannot be undone.
      </DialogContentText>
    </DialogContent>
    <DialogActions>
      <Button onClick={onClose} color='inherit' disabled={isDeleting}>
        Cancel
      </Button>
      <Button onClick={onConfirm} color='error' variant='contained' disabled={isDeleting}>
        {isDeleting ? 'Deleting…' : 'Delete'}
      </Button>
    </DialogActions>
  </Dialog>
)
