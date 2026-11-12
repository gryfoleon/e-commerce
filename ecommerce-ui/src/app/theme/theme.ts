import { alpha, createTheme, type Theme } from '@mui/material/styles'

const slate = {
  50: '#f8fafc',
  100: '#f1f5f9',
  200: '#e2e8f0',
  300: '#cbd5e1',
  500: '#64748b',
  600: '#475569',
  900: '#0f172a'
}

export const appTheme: Theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#4f46e5', light: '#818cf8', dark: '#3730a3', contrastText: '#ffffff' },
    secondary: { main: '#f59e0b', light: '#fbbf24', dark: '#b45309', contrastText: '#0f172a' },
    success: { main: '#16a34a' },
    error: { main: '#dc2626' },
    warning: { main: '#ea580c' },
    info: { main: '#0284c7' },
    background: { default: slate[50], paper: '#ffffff' },
    text: { primary: slate[900], secondary: slate[600] },
    divider: slate[200]
  },
  shape: { borderRadius: 12 },
  typography: {
    fontFamily: [
      'Inter',
      'system-ui',
      '-apple-system',
      'BlinkMacSystemFont',
      '"Segoe UI"',
      'Roboto',
      'sans-serif'
    ].join(','),
    h1: { fontWeight: 700 },
    h2: { fontWeight: 700 },
    h3: { fontWeight: 700 },
    h4: { fontWeight: 700 },
    h5: { fontWeight: 600 },
    h6: { fontWeight: 600 },
    button: { textTransform: 'none', fontWeight: 600 }
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: { root: { borderRadius: 10 } }
    },
    MuiIconButton: {
      styleOverrides: { root: { '&:hover': { backgroundColor: slate[100] } } }
    },
    MuiTextField: { defaultProps: { size: 'small', variant: 'outlined' } },
    MuiOutlinedInput: {
      styleOverrides: {
        root: ({ theme }) => ({
          backgroundColor: theme.palette.background.paper,
          '& fieldset': { borderColor: slate[300] },
          '&:hover fieldset': { borderColor: slate[500] },
          '&.Mui-focused': {
            boxShadow: `0 0 0 3px ${alpha(theme.palette.primary.main, 0.15)}`
          }
        })
      }
    },
    MuiAppBar: {
      defaultProps: { elevation: 0, color: 'inherit' },
      styleOverrides: {
        root: ({ theme }) => ({
          backgroundColor: theme.palette.background.paper,
          borderBottom: `1px solid ${theme.palette.divider}`
        })
      }
    },
    MuiBadge: {
      styleOverrides: {
        badge: ({ theme }) => ({
          fontWeight: 700,
          backgroundColor: theme.palette.secondary.main,
          color: theme.palette.secondary.contrastText
        })
      }
    },
    MuiPaper: { styleOverrides: { root: { backgroundImage: 'none' } } },
    MuiCard: {
      defaultProps: { variant: 'outlined' },
      styleOverrides: { root: { borderColor: slate[200] } }
    },
    MuiChip: { styleOverrides: { root: { fontWeight: 600 } } },
    MuiTableContainer: {
      styleOverrides: {
        root: ({ theme }) => ({
          border: `1px solid ${theme.palette.divider}`,
          borderRadius: theme.shape.borderRadius,
          backgroundColor: theme.palette.background.paper
        })
      }
    },
    MuiTableHead: {
      styleOverrides: {
        root: { backgroundColor: slate[100], '& .MuiTableCell-head': { fontWeight: 700, color: slate[600] } }
      }
    },
    MuiTableRow: {
      styleOverrides: {
        root: ({ theme }) => ({
          '&:hover:not(.MuiTableRow-head)': { backgroundColor: alpha(theme.palette.primary.main, 0.04) },
          '&:last-child td': { borderBottom: 0 }
        })
      }
    },
    MuiTableCell: {
      styleOverrides: { root: { borderColor: slate[200] } }
    }
  }
})
