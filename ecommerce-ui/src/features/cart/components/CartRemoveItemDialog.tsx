import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'
import Typography from '@mui/material/Typography'
import { formatPrice } from '../../../shared/utils/format'
import type { CartItem } from '../stores/cart.store'

interface CartRemoveItemDialogProps {
  readonly item: CartItem | null
  readonly onCancel: () => void
  readonly onConfirm: () => void
}

export const CartRemoveItemDialog = ({ item, onCancel, onConfirm }: CartRemoveItemDialogProps) => (
  <Dialog open={item !== null} onClose={onCancel} fullWidth maxWidth='xs' aria-labelledby='cart-remove-title'>
    <DialogTitle id='cart-remove-title'>Remove product</DialogTitle>
    <DialogContent>
      <DialogContentText sx={{ mb: 2 }}>Are you sure you want to stop buying this product?</DialogContentText>
      <Typography sx={{ fontWeight: 600 }}>{item?.name}</Typography>
      <Typography variant='body2' color='text.secondary' sx={{ my: 0.5 }}>
        {item?.description ?? 'No description'}
      </Typography>
      <Typography variant='body2'>Unit price: {item ? formatPrice(item.unitPrice) : ''}</Typography>
    </DialogContent>
    <DialogActions>
      <Button onClick={onCancel} color='inherit'>
        Cancel
      </Button>
      <Button onClick={onConfirm} color='error' variant='contained'>
        Remove
      </Button>
    </DialogActions>
  </Dialog>
)
