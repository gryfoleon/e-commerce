import Box from '@mui/material/Box'
import CircularProgress from '@mui/material/CircularProgress'
import { alpha } from '@mui/material/styles'

interface LoaderProps {
  readonly label?: string
  readonly overlay?: boolean
  readonly fullScreen?: boolean
  readonly size?: number
}

export const Loader = ({ label = 'Loading', overlay = false, fullScreen = false, size = 40 }: LoaderProps) => (
  <Box
    role='status'
    aria-label={label}
    sx={
      overlay || fullScreen
        ? {
            position: fullScreen ? 'fixed' : 'absolute',
            inset: 0,
            zIndex: (theme) => (fullScreen ? theme.zIndex.modal + 1 : 1),
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            bgcolor: (theme) => alpha(theme.palette.background.paper, 0.6)
          }
        : { display: 'flex', justifyContent: 'center', py: 4 }
    }
  >
    <CircularProgress size={size} aria-hidden />
  </Box>
)
