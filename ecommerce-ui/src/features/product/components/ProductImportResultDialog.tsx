import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import type { CsvImportReport } from '../api/product.dto'

interface ProductImportResultDialogProps {
  readonly report: CsvImportReport | null
  readonly onClose: () => void
}

const summaryOf = (report: CsvImportReport): ReadonlyArray<[string, number]> => [
  ['Total rows', report.totalRows],
  ['Inserted', report.insertedRows],
  ['Updated', report.updatedRows],
  ['Rejected', report.rejectedRows]
]

const resultAlert = (report: CsvImportReport) => {
  if (report.aborted) return <Alert severity='error'>The import was aborted. Review the errors below.</Alert>
  if (report.rejectedRows > 0) return <Alert severity='warning'>The import finished with rejected rows.</Alert>
  return <Alert severity='success'>The import finished successfully.</Alert>
}

export const ProductImportResultDialog = ({ report, onClose }: ProductImportResultDialogProps) => (
  <Dialog open={report !== null} onClose={onClose} fullWidth maxWidth='md' aria-labelledby='product-import-result-title'>
    <DialogTitle id='product-import-result-title'>CSV import result</DialogTitle>
    {report && (
      <DialogContent dividers sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        {resultAlert(report)}
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 2 }}>
          {summaryOf(report).map(([label, value]) => (
            <Box key={label}>
              <Typography variant='caption' color='text.secondary'>
                {label}
              </Typography>
              <Typography variant='h6'>{value}</Typography>
            </Box>
          ))}
        </Box>
        <Typography variant='body2' color='text.secondary'>
          Processed in {report.durationMs} ms
        </Typography>
        {report.errors.length > 0 && (
          <>
            <Typography variant='subtitle2'>Errors</Typography>
            <TableContainer sx={{ maxHeight: 320 }}>
              <Table size='small' stickyHeader aria-label='import errors'>
                <TableHead>
                  <TableRow>
                    <TableCell>Line</TableCell>
                    <TableCell>SKU</TableCell>
                    <TableCell>Field</TableCell>
                    <TableCell>Value</TableCell>
                    <TableCell>Message</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {report.errors.map((error, index) => (
                    <TableRow key={`${error.line}-${error.field ?? ''}-${index}`}>
                      <TableCell>{error.line}</TableCell>
                      <TableCell>{error.sku ?? '—'}</TableCell>
                      <TableCell>{error.field ?? '—'}</TableCell>
                      <TableCell>{error.rejectedValue ?? '—'}</TableCell>
                      <TableCell>{error.message}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            {report.errorsTruncated && (
              <Typography variant='body2' color='text.secondary'>
                Only the first {report.errors.length} errors are shown.
              </Typography>
            )}
          </>
        )}
      </DialogContent>
    )}
    <DialogActions>
      <Button onClick={onClose} variant='contained'>
        Close
      </Button>
    </DialogActions>
  </Dialog>
)
