import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutlineOutlined'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Typography from '@mui/material/Typography'

interface CheckoutSuccessDialogProps {
  readonly open: boolean
  readonly onClose: () => void
}

export const CheckoutSuccessDialog = ({ open, onClose }: CheckoutSuccessDialogProps) => (
  <Dialog open={open} onClose={onClose} fullWidth maxWidth='xs' aria-labelledby='checkout-success-title'>
    <DialogTitle id='checkout-success-title' sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
      <CheckCircleOutlineIcon color='success' />
      Compra exitosa
    </DialogTitle>
    <DialogContent>
      <Typography>Your purchase was completed successfully.</Typography>
    </DialogContent>
    <DialogActions>
      <Button onClick={onClose} variant='contained'>
        Close
      </Button>
    </DialogActions>
  </Dialog>
)
