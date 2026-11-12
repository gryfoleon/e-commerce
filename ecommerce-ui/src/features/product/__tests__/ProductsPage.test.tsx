import { ThemeProvider } from '@mui/material/styles'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import App from '../../../App'
import { appTheme } from '../../../app/theme/theme'
import { API_BASE_URL } from '../../../core/http/api-client'
import { server } from '../../../mocks/node'
import { CATEGORIES_URL } from '../../../mocks/handlers'
import { PRODUCTS_ENDPOINT } from '../api/product.api'
import type { CsvImportReport, PageableResponse, ProductDto, ProductSearchDto, UUID } from '../api/product.dto'
import { EMPTY_FILTERS, useProductFilterStore } from '../stores/product-filter.store'

const FILTER_URL = `${API_BASE_URL}${PRODUCTS_ENDPOINT}/filter`
const IMPORT_URL = `${API_BASE_URL}${PRODUCTS_ENDPOINT}/import-csv`

const item = (n: number): ProductSearchDto => ({
  id: `00000000-0000-4000-8000-${String(n).padStart(12, '0')}` as UUID,
  sku: `ELEC${String(n).padStart(3, '0')}`,
  name: `Product ${n}`,
  description: null,
  category: 'Electronics',
  price: 10 + n,
  stock: n,
  weightKg: null
})

const pageOf = (content: ProductSearchDto[]): PageableResponse<ProductSearchDto> => ({
  content,
  pageDetails: { page: 0, size: 15, totalPages: 1, totalElements: content.length, isLast: true }
})

let requests: URLSearchParams[] = []

const mockSearch = (content: ProductSearchDto[] = [item(1), item(2)]) =>
  server.use(
    http.get(FILTER_URL, ({ request }) => {
      requests.push(new URL(request.url).searchParams)
      return HttpResponse.json(pageOf(content))
    })
  )

const renderApp = () =>
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false, staleTime: 300_000 } } })}>
      <ThemeProvider theme={appTheme}>
        <App />
      </ThemeProvider>
    </QueryClientProvider>
  )

const filterForm = () => screen.getByRole('search', { name: 'Product filters' })
const headerSearch = () => screen.getByRole('textbox', { name: 'Search products' })

describe('ProductsPage', () => {
  beforeEach(() => {
    requests = []
    useProductFilterStore.getState().clearFilters()
  })

  it('loads the first page from the API into the table', async () => {
    mockSearch()
    renderApp()
    expect(await screen.findByText('ELEC001')).toBeInTheDocument()
    expect(screen.getByText('Product 2')).toBeInTheDocument()
    expect(screen.getByText(/1–2 of 2/)).toBeInTheDocument()
    expect(requests).toHaveLength(1)
    expect(Object.fromEntries(requests[0])).toEqual({ page: '0', size: '15' })
  })

  it('shows the category column between description and price', async () => {
    mockSearch()
    renderApp()
    await screen.findByText('ELEC001')
    const headers = screen.getAllByRole('columnheader').map((h) => h.textContent)
    expect(headers.slice(headers.indexOf('Description'), headers.indexOf('Price') + 1)).toEqual([
      'Description',
      'Category',
      'Price'
    ])
    const row = screen.getByText('ELEC001').closest('tr')!
    const cells = within(row).getAllByRole('cell')
    expect(cells[headers.indexOf('Category')]).toHaveTextContent('Electronics')
  })

  it('shows the loader while the request is in flight', async () => {
    server.use(
      http.get(FILTER_URL, async () => {
        await delay(100)
        return HttpResponse.json(pageOf([item(1)]))
      })
    )
    renderApp()
    expect(screen.getByRole('status', { name: 'Loading products' })).toBeInTheDocument()
    expect(screen.queryByText('No products found')).not.toBeInTheDocument()
    await screen.findByText('ELEC001')
    expect(screen.queryByRole('status', { name: 'Loading products' })).not.toBeInTheDocument()
  })

  it('shows the empty state when the API returns no products', async () => {
    mockSearch([])
    renderApp()
    expect(await screen.findByText('No products found')).toBeInTheDocument()
    expect(screen.getByText(/0–0 of 0/)).toBeInTheDocument()
  })

  it('shows an error when the list cannot be loaded', async () => {
    server.use(http.get(FILTER_URL, () => HttpResponse.json({}, { status: 500 })))
    renderApp()
    expect(await screen.findByText('Could not load products.')).toBeInTheDocument()
  })

  it('searches from the header with at least 3 characters on Enter', async () => {
    mockSearch()
    renderApp()
    await screen.findByText('ELEC001')

    await userEvent.type(headerSearch(), 'ab{Enter}')
    expect(screen.getByRole('button', { name: 'Search' })).toBeDisabled()
    expect(requests).toHaveLength(1)

    await userEvent.type(headerSearch(), 'c{Enter}')
    await waitFor(() => expect(requests).toHaveLength(2))
    expect(requests[1].get('name')).toBe('abc')
    expect(requests[1].get('page')).toBe('0')
  })

  it('repeats the same header search when clicking the search button', async () => {
    mockSearch()
    renderApp()
    await screen.findByText('ELEC001')
    await userEvent.type(headerSearch(), 'mouse{Enter}')
    await waitFor(() => expect(requests).toHaveLength(2))

    await userEvent.click(screen.getByRole('button', { name: 'Search' }))
    await waitFor(() => expect(requests).toHaveLength(3))
    expect(requests[2].get('name')).toBe('mouse')
  })

  it('sends the applied filters and repeats the request on every Apply filters click', async () => {
    mockSearch()
    renderApp()
    await screen.findByText('ELEC001')
    await userEvent.type(within(filterForm()).getByLabelText('SKU'), 'ELEC')
    await userEvent.type(screen.getByLabelText('Minimum price'), '5')
    await userEvent.click(screen.getByRole('button', { name: 'Apply filters' }))

    await waitFor(() => expect(requests).toHaveLength(2))
    expect(Object.fromEntries(requests[1])).toEqual({ sku: 'ELEC', minPrice: '5', page: '0', size: '15' })

    await userEvent.click(screen.getByRole('button', { name: 'Apply filters' }))
    await waitFor(() => expect(requests).toHaveLength(3))
    expect(Object.fromEntries(requests[2])).toEqual(Object.fromEntries(requests[1]))
  })

  it('sends minStock for the selected stock state', async () => {
    mockSearch()
    renderApp()
    await screen.findByText('ELEC001')
    const stockSelect = within(filterForm())
      .getAllByRole('combobox')
      .find((element) => element.textContent === 'Any')!
    await userEvent.click(stockSelect)
    await userEvent.click(screen.getByRole('option', { name: 'In stock' }))
    await userEvent.click(screen.getByRole('button', { name: 'Apply filters' }))

    await waitFor(() => expect(requests).toHaveLength(2))
    expect(requests[1].get('minStock')).toBe('1')
  })

  it('loads the category options from the API and filters by the selected one', async () => {
    mockSearch()
    renderApp()
    await screen.findByText('ELEC001')
    const categorySelect = within(filterForm())
      .getAllByRole('combobox')
      .find((element) => element.textContent === 'All')!
    await userEvent.click(categorySelect)
    const listbox = screen.getByRole('listbox')
    expect(within(listbox).getAllByRole('option').map((o) => o.textContent)).toEqual(['All', 'Audio', 'Electronics'])
    await userEvent.click(within(listbox).getByRole('option', { name: 'Audio' }))
    await userEvent.click(screen.getByRole('button', { name: 'Apply filters' }))

    await waitFor(() => expect(requests).toHaveLength(2))
    expect(requests[1].get('category')).toBe('Audio')
  })

  it('tells the user when the categories cannot be loaded', async () => {
    mockSearch()
    server.use(http.get(CATEGORIES_URL, () => HttpResponse.json({}, { status: 500 })))
    renderApp()
    await screen.findByText('ELEC001')
    const categorySelect = within(filterForm())
      .getAllByRole('combobox')
      .find((element) => element.textContent === 'All')!
    await userEvent.click(categorySelect)
    expect(await screen.findByText('Could not load categories')).toBeInTheDocument()
  })

  it('opens the add product dialog', async () => {
    mockSearch()
    renderApp()
    await userEvent.click(screen.getByRole('button', { name: 'Add product' }))
    const dialog = screen.getByRole('dialog')
    expect(within(dialog).getByText('Add product')).toBeInTheDocument()
    expect(within(dialog).getByLabelText('SKU')).toHaveValue('')
  })

  it('clears the filters', async () => {
    mockSearch()
    renderApp()
    await screen.findByText('ELEC001')
    await userEvent.type(within(filterForm()).getByLabelText('SKU'), 'ELEC')
    await userEvent.click(screen.getByRole('button', { name: 'Apply filters' }))
    await waitFor(() => expect(requests).toHaveLength(2))
    await userEvent.click(within(filterForm()).getByRole('button', { name: 'Clear filters' }))
    expect(useProductFilterStore.getState().applied).toEqual(EMPTY_FILTERS)
    expect(within(filterForm()).getByLabelText('SKU')).toHaveValue('')
    expect(requests).toHaveLength(2)
  })

  describe('CSV import', () => {
    const report: CsvImportReport = {
      totalRows: 4,
      insertedRows: 2,
      updatedRows: 1,
      rejectedRows: 1,
      errorsTruncated: false,
      aborted: false,
      durationMs: 42,
      errors: [{ line: 4, sku: 'BAD01', field: 'price', rejectedValue: '-1', message: 'Price must be positive' }]
    }

    const uploadCsv = () =>
      userEvent.upload(screen.getByTestId('csv-input'), new File(['sku,name\n'], 'products.csv', { type: 'text/csv' }))

    it('uploads the file, shows the loader and then the result modal', async () => {
      let uploadedBody = ''
      let contentType: string | null = null
      mockSearch()
      server.use(
        http.post(IMPORT_URL, async ({ request }) => {
          contentType = request.headers.get('content-type')
          uploadedBody = await request.text()
          await delay(100)
          return HttpResponse.json(report)
        })
      )
      renderApp()
      await screen.findByText('ELEC001')
      await uploadCsv()

      expect(await screen.findByRole('status', { name: 'Importing products' })).toBeInTheDocument()
      const dialog = await screen.findByRole('dialog', { name: 'CSV import result' })
      expect(screen.queryByRole('status', { name: 'Importing products' })).not.toBeInTheDocument()
      expect(within(dialog).getByText('The import finished with rejected rows.')).toBeInTheDocument()
      expect(within(dialog).getByText('Inserted').nextSibling).toHaveTextContent('2')
      expect(within(dialog).getByText('Updated').nextSibling).toHaveTextContent('1')
      expect(within(dialog).getByText('Rejected').nextSibling).toHaveTextContent('1')
      expect(within(dialog).getByText('Price must be positive')).toBeInTheDocument()
      expect(contentType).toMatch(/^multipart\/form-data/)
      expect(uploadedBody).toContain('name="file"; filename="products.csv"')
      await waitFor(() => expect(requests).toHaveLength(2))

      await userEvent.click(within(dialog).getByRole('button', { name: 'Close' }))
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })

    it('shows the backend error on screen when the import fails', async () => {
      mockSearch()
      server.use(
        http.post(IMPORT_URL, () =>
          HttpResponse.json(
            {
              statusCode: 422,
              response: 'Unprocessable Content',
              timestamp: '2026-11-11T00:00:00Z',
              error: 'Ingestion error',
              errorDetails: [{ message: 'Line 2: price must be positive' }, { message: 'Line 3: sku is required' }]
            },
            { status: 422 }
          )
        )
      )
      renderApp()
      await screen.findByText('ELEC001')
      await uploadCsv()

      const alert = await screen.findByText('Ingestion error')
      const container = alert.closest('[role="alert"]') as HTMLElement
      expect(within(container).getByText('Line 2: price must be positive')).toBeInTheDocument()
      expect(within(container).getByText('Line 3: sku is required')).toBeInTheDocument()
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

      await userEvent.click(within(container).getByRole('button', { name: /close/i }))
      expect(screen.queryByText('Ingestion error')).not.toBeInTheDocument()
    })

    it('shows a generic error when the server is unreachable and still refreshes the table', async () => {
      mockSearch()
      server.use(http.post(IMPORT_URL, () => HttpResponse.error()))
      renderApp()
      await screen.findByText('ELEC001')
      await uploadCsv()
      expect(await screen.findByText('The CSV file could not be imported.')).toBeInTheDocument()
      await waitFor(() => expect(requests).toHaveLength(2))
    })
  })

  describe('delete', () => {
    const mockCatalog = () => {
      let catalog = [item(1), item(2), item(3)]
      const deleted: string[] = []
      server.use(
        http.get(FILTER_URL, ({ request }) => {
          requests.push(new URL(request.url).searchParams)
          return HttpResponse.json(pageOf(catalog))
        }),
        http.delete(`${API_BASE_URL}${PRODUCTS_ENDPOINT}/:idOrSku`, ({ params }) => {
          deleted.push(String(params.idOrSku))
          catalog = catalog.filter((product) => product.id !== params.idOrSku)
          return new HttpResponse(null, { status: 204 })
        })
      )
      return deleted
    }

    it('deletes the product after confirming and refreshes the table', async () => {
      const deleted = mockCatalog()
      renderApp()
      await userEvent.click(await screen.findByRole('button', { name: 'Delete Product 2' }))

      const dialog = screen.getByRole('dialog', { name: 'Delete product' })
      expect(within(dialog).getByText(/Are you sure/)).toHaveTextContent('Product 2 (ELEC002)')
      await userEvent.click(within(dialog).getByRole('button', { name: 'Delete' }))

      await waitFor(() => expect(screen.queryByText('ELEC002')).not.toBeInTheDocument())
      expect(deleted).toEqual([item(2).id])
      expect(requests).toHaveLength(2)
      expect(screen.getByText(/1–2 of 2/)).toBeInTheDocument()
      expect(screen.getByText('Product ELEC002 deleted')).toBeInTheDocument()
      await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Delete product' })).not.toBeInTheDocument())
    })

    it('does not delete when the confirmation is cancelled', async () => {
      const deleted = mockCatalog()
      renderApp()
      await userEvent.click(await screen.findByRole('button', { name: 'Delete Product 1' }))
      await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }))

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      expect(deleted).toEqual([])
      expect(screen.getByText('ELEC001')).toBeInTheDocument()
      expect(requests).toHaveLength(1)
    })

    it('shows the backend error and keeps the product when the delete fails', async () => {
      mockSearch()
      server.use(
        http.delete(`${API_BASE_URL}${PRODUCTS_ENDPOINT}/:idOrSku`, () =>
          HttpResponse.json(
            { statusCode: 404, response: 'Not Found', timestamp: '2026-11-11T00:00:00Z', error: 'Product not found' },
            { status: 404 }
          )
        )
      )
      renderApp()
      await userEvent.click(await screen.findByRole('button', { name: 'Delete Product 1' }))
      const dialog = screen.getByRole('dialog')
      await userEvent.click(within(dialog).getByRole('button', { name: 'Delete' }))

      expect(await screen.findByText('Product not found')).toBeInTheDocument()
      expect(within(dialog).getByRole('button', { name: 'Delete' })).toBeEnabled()
      expect(screen.getByText('ELEC001')).toBeInTheDocument()
    })
  })

  describe('create', () => {
    type CreateCall = { body: unknown; idempotencyKey: string | null }

    const mockCreate = (respond?: (body: Record<string, unknown>) => Response) => {
      let catalog = [item(1)]
      const calls: CreateCall[] = []
      server.use(
        http.get(FILTER_URL, ({ request }) => {
          requests.push(new URL(request.url).searchParams)
          return HttpResponse.json(pageOf(catalog))
        }),
        http.post(`${API_BASE_URL}${PRODUCTS_ENDPOINT}`, async ({ request }) => {
          const body = (await request.json()) as Record<string, unknown>
          calls.push({ body, idempotencyKey: request.headers.get('Idempotency-Key') })
          const failure = respond?.(body)
          if (failure) return failure
          const created = { ...item(2), ...body, id: item(2).id } as ProductSearchDto
          catalog = [created, ...catalog]
          return HttpResponse.json(
            { ...created, version: 0, createdAt: '2026-11-11T00:00:00Z', updatedAt: '2026-11-11T00:00:00Z' },
            { status: 201, headers: { ETag: '"0"' } }
          )
        })
      )
      return calls
    }

    const fillForm = async (dialog: HTMLElement, values: Partial<Record<string, string>> = {}) => {
      const { Category: category, ...fields } = {
        SKU: 'new01',
        Category: 'Audio',
        Name: 'Speaker',
        'Price ($)': '99.5',
        Stock: '3',
        ...values
      }
      for (const [label, value] of Object.entries(fields)) {
        const input = within(dialog).getByLabelText(label)
        await userEvent.clear(input)
        if (value) await userEvent.type(input, value)
      }
      if (category) {
        await userEvent.click(within(dialog).getByRole('combobox', { name: 'Category' }))
        await userEvent.click(await screen.findByRole('option', { name: category }))
      }
    }

    const openAddDialog = async () => {
      await screen.findByText('ELEC001')
      await userEvent.click(screen.getByRole('button', { name: 'Add product' }))
      return screen.getByRole('dialog')
    }

    it('creates the product, closes the modal and refreshes the table', async () => {
      const calls = mockCreate()
      renderApp()
      const dialog = await openAddDialog()
      await fillForm(dialog)
      await userEvent.click(within(dialog).getByRole('button', { name: 'Create product' }))

      expect(await screen.findByText('Product NEW01 created')).toBeInTheDocument()
      expect(calls).toHaveLength(1)
      expect(calls[0].body).toEqual({
        sku: 'NEW01',
        name: 'Speaker',
        description: null,
        category: 'Audio',
        price: 99.5,
        stock: 3,
        weightKg: null
      })
      expect(calls[0].idempotencyKey).toMatch(/^[0-9a-f-]{36}$/)
      expect(await screen.findByText('NEW01')).toBeInTheDocument()
      expect(screen.getByText(/1–2 of 2/)).toBeInTheDocument()
      expect(requests).toHaveLength(2)
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })

    it('validates the form before calling the API', async () => {
      const calls = mockCreate()
      renderApp()
      const dialog = await openAddDialog()
      await fillForm(dialog, { SKU: 'AB-1', 'Price ($)': '0' })
      await userEvent.click(within(dialog).getByRole('button', { name: 'Create product' }))

      expect(
        await within(dialog).findByText('SKU must have 1 to 10 characters, only uppercase letters and digits')
      ).toBeInTheDocument()
      expect(within(dialog).getByText('Price must be greater than zero')).toBeInTheDocument()
      expect(calls).toHaveLength(0)
    })

    it('offers only catalog categories and requires one', async () => {
      const calls = mockCreate()
      renderApp()
      const dialog = await openAddDialog()
      await fillForm(dialog, { Category: '' })
      await userEvent.click(within(dialog).getByRole('combobox', { name: 'Category' }))
      const listbox = screen.getByRole('listbox')
      expect(within(listbox).getAllByRole('option').map((o) => o.textContent)).toEqual([
        'Select a category',
        'Audio',
        'Electronics'
      ])
      await userEvent.keyboard('{Escape}')
      await userEvent.click(within(dialog).getByRole('button', { name: 'Create product' }))

      expect(await within(dialog).findByText('Category is required')).toBeInTheDocument()
      expect(calls).toHaveLength(0)
    })

    it('shows backend field errors in the form and keeps it open', async () => {
      const calls = mockCreate(() =>
        HttpResponse.json(
          {
            statusCode: 409,
            response: 'Conflict',
            timestamp: '2026-11-11T00:00:00Z',
            error: 'Resource conflict',
            errorDetails: [{ field: 'sku', rejectedValue: 'NEW01', message: 'SKU NEW01 already exists' }]
          },
          { status: 409 }
        )
      )
      renderApp()
      const dialog = await openAddDialog()
      await fillForm(dialog)
      await userEvent.click(within(dialog).getByRole('button', { name: 'Create product' }))

      expect(await within(dialog).findByText('SKU NEW01 already exists')).toBeInTheDocument()
      expect(screen.getByText('Resource conflict')).toBeInTheDocument()
      expect(screen.getByRole('dialog')).toBeInTheDocument()
      expect(requests).toHaveLength(1)

      await userEvent.click(within(dialog).getByRole('button', { name: 'Create product' }))
      await waitFor(() => expect(calls).toHaveLength(2))
      expect(calls[1].idempotencyKey).toBe(calls[0].idempotencyKey)

      await fillForm(dialog, { SKU: 'NEW02' })
      await userEvent.click(within(dialog).getByRole('button', { name: 'Create product' }))
      await waitFor(() => expect(calls).toHaveLength(3))
      expect(calls[2].idempotencyKey).not.toBe(calls[0].idempotencyKey)
    })
  })

  describe('edit', () => {
    type UpdateCall = { id: string; body: Record<string, unknown>; ifMatch: string | null }

    const PRODUCT_URL = `${API_BASE_URL}${PRODUCTS_ENDPOINT}/:id`

    const mockBackend = (options: { conflictOnce?: boolean } = {}) => {
      let stored: ProductDto = {
        ...item(1),
        description: 'Original description',
        weightKg: 1.5,
        version: 3,
        createdAt: '2026-11-11T00:00:00Z',
        updatedAt: '2026-11-11T00:00:00Z'
      }
      const toRow = ({ version: _v, createdAt: _c, updatedAt: _u, ...row }: ProductDto): ProductSearchDto => row
      const updates: UpdateCall[] = []
      let detailRequests = 0
      let conflictPending = options.conflictOnce ?? false
      server.use(
        http.get(FILTER_URL, ({ request }) => {
          requests.push(new URL(request.url).searchParams)
          return HttpResponse.json(pageOf([toRow(stored), item(2)]))
        }),
        http.get(PRODUCT_URL, () => {
          detailRequests += 1
          return HttpResponse.json(stored, { headers: { ETag: `"${stored.version}"` } })
        }),
        http.put(PRODUCT_URL, async ({ params, request }) => {
          const body = (await request.json()) as Record<string, unknown>
          updates.push({ id: String(params.id), body, ifMatch: request.headers.get('If-Match') })
          if (conflictPending) {
            conflictPending = false
            stored = { ...stored, name: 'Changed elsewhere', version: stored.version + 1 }
            return HttpResponse.json(
              { statusCode: 409, response: 'Conflict', timestamp: '2026-11-11T00:00:00Z', error: 'Version mismatch' },
              { status: 409 }
            )
          }
          stored = { ...stored, ...body, version: stored.version + 1 } as ProductDto
          return HttpResponse.json(stored, { headers: { ETag: `"${stored.version}"` } })
        })
      )
      return { updates, detailRequests: () => detailRequests }
    }

    const openEditDialog = async (name = 'Product 1') => {
      await userEvent.click(await screen.findByRole('button', { name: `Edit ${name}` }))
      return screen.getByRole('dialog')
    }

    it('prefills the form with the product loaded from the API', async () => {
      const backend = mockBackend()
      renderApp()
      const dialog = await openEditDialog()

      expect(within(dialog).getByText('Edit product')).toBeInTheDocument()
      expect(await within(dialog).findByLabelText('SKU')).toHaveValue('ELEC001')
      expect(within(dialog).getByLabelText('SKU')).toHaveAttribute('readonly')
      expect(within(dialog).getByLabelText('Name')).toHaveValue('Product 1')
      expect(within(dialog).getByLabelText('Description')).toHaveValue('Original description')
      expect(within(dialog).getByLabelText('Price ($)')).toHaveValue('11')
      expect(within(dialog).getByLabelText('Weight (kg)')).toHaveValue('1.5')
      expect(within(dialog).getByRole('combobox', { name: 'Category' })).toHaveTextContent('Electronics')
      expect(backend.detailRequests()).toBe(1)
    })

    it('saves the changes with the ETag and refreshes the table', async () => {
      const { updates } = mockBackend()
      renderApp()
      const dialog = await openEditDialog()
      const name = await within(dialog).findByLabelText('Name')
      await userEvent.clear(name)
      await userEvent.type(name, 'Renamed product')
      await userEvent.clear(within(dialog).getByLabelText('Price ($)'))
      await userEvent.type(within(dialog).getByLabelText('Price ($)'), '20.25')
      await userEvent.click(within(dialog).getByRole('button', { name: 'Save changes' }))

      expect(await screen.findByText('Product ELEC001 updated')).toBeInTheDocument()
      expect(updates).toEqual([
        {
          id: item(1).id,
          ifMatch: '"3"',
          body: {
            sku: 'ELEC001',
            name: 'Renamed product',
            description: 'Original description',
            category: 'Electronics',
            price: 20.25,
            stock: 1,
            weightKg: 1.5
          }
        }
      ])
      expect(await screen.findByText('Renamed product')).toBeInTheDocument()
      expect(screen.getByText('$20.25')).toBeInTheDocument()
      expect(requests).toHaveLength(2)
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })

    it('shows the new category in the table after changing it', async () => {
      const { updates } = mockBackend()
      renderApp()
      const dialog = await openEditDialog()
      await userEvent.click(await within(dialog).findByRole('combobox', { name: 'Category' }))
      await userEvent.click(await screen.findByRole('option', { name: 'Audio' }))
      await userEvent.click(within(dialog).getByRole('button', { name: 'Save changes' }))

      expect(await screen.findByText('Product ELEC001 updated')).toBeInTheDocument()
      expect(updates[0].body.category).toBe('Audio')
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      const row = screen.getByText('ELEC001').closest('tr')!
      await waitFor(() => expect(within(row).getByText('Audio')).toBeInTheDocument())
    })

    it('shows an error when the product cannot be loaded', async () => {
      server.use(http.get(PRODUCT_URL, () => HttpResponse.json({}, { status: 500 })))
      mockSearch()
      renderApp()
      const dialog = await openEditDialog()
      expect(await within(dialog).findByText('Could not load the product.')).toBeInTheDocument()
      expect(within(dialog).queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument()
    })

    it('reloads the latest version when someone else changed the product', async () => {
      const { updates } = mockBackend({ conflictOnce: true })
      renderApp()
      const dialog = await openEditDialog()
      const name = await within(dialog).findByLabelText('Name')
      await userEvent.clear(name)
      await userEvent.type(name, 'My change')
      await userEvent.click(within(dialog).getByRole('button', { name: 'Save changes' }))

      expect(await screen.findByText(/modified by someone else/)).toBeInTheDocument()
      await waitFor(() => expect(within(screen.getByRole('dialog')).getByLabelText('Name')).toHaveValue('Changed elsewhere'))

      await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Save changes' }))
      await waitFor(() => expect(updates).toHaveLength(2))
      expect(updates.map((update) => update.ifMatch)).toEqual(['"3"', '"4"'])
      expect(await screen.findByText('Product ELEC001 updated')).toBeInTheDocument()
    })
  })
})
