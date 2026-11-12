import { zodResolver } from '@hookform/resolvers/zod'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Grid from '@mui/material/Grid'
import MenuItem from '@mui/material/MenuItem'
import TextField from '@mui/material/TextField'
import { Controller, useForm } from 'react-hook-form'
import { Loader } from '../../../shared/components/Loader'
import { useCategoryOptionsQuery } from '../../category/hooks/category.query'
import { withSelectedOption } from '../../category/util/category.utils'
import { productFormSchema, type ProductFormValues } from '../model/product.schema'
import { toFormValues, toProductInput } from '../util/product.utils'
import type { Product, ProductInput } from '../types/product.types'

type FormProduct = Product & { readonly version?: number }

interface ProductFormDialogProps {
  readonly open: boolean
  readonly mode?: 'create' | 'edit'
  readonly product: FormProduct | null
  readonly isLoading?: boolean
  readonly loadError?: string | null
  readonly isSubmitting?: boolean
  readonly onClose: () => void
  readonly onSubmit: (input: ProductInput) => void
  readonly serverErrors?: ProductFormServerErrors
}

export type ProductFormServerErrors = Partial<Record<keyof ProductFormValues, string>>

interface FormBodyProps extends Omit<ProductFormDialogProps, 'open' | 'mode' | 'isLoading' | 'loadError'> {
  readonly isEdit: boolean
}

const FormBody = ({ product, isEdit, isSubmitting, onClose, onSubmit, serverErrors = {} }: FormBodyProps) => {
  const {
    control,
    register,
    handleSubmit,
    formState: { errors }
  } = useForm<ProductFormValues>({
    resolver: zodResolver(productFormSchema),
    defaultValues: toFormValues(product ?? undefined)
  })
  const { options: categoryOptions, isPending: categoriesLoading, isError: categoriesFailed } = useCategoryOptionsQuery()

  const field = (name: keyof ProductFormValues, label: string, extra: object = {}) => {
    const message = errors[name]?.message ?? serverErrors[name]
    return <TextField fullWidth label={label} error={Boolean(message)} helperText={message} {...register(name)} {...extra} />
  }

  return (
    <form onSubmit={handleSubmit((values) => onSubmit(toProductInput(values)))} noValidate>
      <DialogTitle>{isEdit ? 'Edit product' : 'Add product'}</DialogTitle>
      <DialogContent dividers>
        <Grid container spacing={2} sx={{ pt: 1 }}>
          <Grid size={{ xs: 12, sm: 6 }}>{field('sku', 'SKU', {
              ...register('sku', { setValueAs: (value: string) => value.toUpperCase() }),
              slotProps: { htmlInput: { maxLength: 10, readOnly: isEdit, style: { textTransform: 'uppercase' } } }
            })}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <Controller
              control={control}
              name='category'
              render={({ field: { ref, ...categoryField } }) => {
                const message = errors.category?.message ?? serverErrors.category
                return (
                  <TextField
                    select
                    fullWidth
                    label='Category'
                    error={Boolean(message)}
                    helperText={message}
                    inputRef={ref}
                    {...categoryField}
                    slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }}
                  >
                    <MenuItem value='' disabled>
                      Select a category
                    </MenuItem>
                    {withSelectedOption(categoryOptions, categoryField.value).map((o) => (
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
                )
              }}
            />
          </Grid>
          <Grid size={12}>{field('name', 'Name')}</Grid>
          <Grid size={12}>{field('description', 'Description', { multiline: true, minRows: 3 })}</Grid>
          <Grid size={{ xs: 12, sm: 4 }}>{field('price', 'Price ($)', { inputMode: 'decimal' })}</Grid>
          <Grid size={{ xs: 12, sm: 4 }}>{field('stock', 'Stock', { inputMode: 'numeric' })}</Grid>
          <Grid size={{ xs: 12, sm: 4 }}>{field('weightKg', 'Weight (kg)', { inputMode: 'decimal' })}</Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color='inherit' disabled={isSubmitting}>
          Cancel
        </Button>
        <Button type='submit' variant='contained' disabled={isSubmitting}>
          {isSubmitting ? 'Saving…' : isEdit ? 'Save changes' : 'Create product'}
        </Button>
      </DialogActions>
    </form>
  )
}

const LoadingBody = ({ loadError, onClose }: Pick<ProductFormDialogProps, 'loadError' | 'onClose'>) => (
  <>
    <DialogTitle>Edit product</DialogTitle>
    <DialogContent dividers>
      {loadError ? <Alert severity='error'>{loadError}</Alert> : <Loader label='Loading product' />}
    </DialogContent>
    <DialogActions>
      <Button onClick={onClose} color='inherit'>
        Cancel
      </Button>
    </DialogActions>
  </>
)

export const ProductFormDialog = ({
  open,
  mode = 'create',
  product,
  isLoading = false,
  loadError = null,
  ...rest
}: ProductFormDialogProps) => {
  const isEdit = mode === 'edit'
  const showForm = !isEdit || (product !== null && !isLoading && !loadError)
  return (
    <Dialog
      open={open}
      onClose={rest.isSubmitting ? undefined : rest.onClose}
      fullWidth
      maxWidth='sm'
      slotProps={{ transition: { unmountOnExit: true } }}
    >
      {showForm ? (
        <FormBody key={product ? `${product.id}:${product.version ?? ''}` : 'new'} product={product} isEdit={isEdit} {...rest} />
      ) : (
        <LoadingBody loadError={loadError} onClose={rest.onClose} />
      )}
    </Dialog>
  )
}
