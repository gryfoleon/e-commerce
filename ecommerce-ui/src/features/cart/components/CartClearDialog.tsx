import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'

interface CartClearDialogProps {
  readonly open: boolean
  readonly onCancel: () => void
  readonly onConfirm: () => void
}

export const CartClearDialog = ({ open, onCancel, onConfirm }: CartClearDialogProps) => (
  <Dialog open={open} onClose={onCancel} fullWidth maxWidth='xs' aria-labelledby='cart-clear-title'>
    <DialogTitle id='cart-clear-title'>Clear cart</DialogTitle>
    <DialogContent>
      <DialogContentText>Are you sure you want to remove all products from the cart?</DialogContentText>
    </DialogContent>
    <DialogActions>
      <Button onClick={onCancel} color='inherit'>
        Cancel
      </Button>
      <Button onClick={onConfirm} color='error' variant='contained'>
        Clear cart
      </Button>
    </DialogActions>
  </Dialog>
)
