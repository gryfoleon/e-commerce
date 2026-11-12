import { AxiosHeaders, type AxiosResponse } from 'axios'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ZodError } from 'zod'
import {
  CATEGORIES_ENDPOINT,
  categoryApiClient,
  getCategories,
  getCategoryById,
  getCategoryByName
} from '../api/category.api'
import type { CategoryDto, CategoryListParams, CategoryPage } from '../api/category.dto'

const category: CategoryDto = { id: 5, name: 'Electronics', createdAt: '2026-11-11T20:00:00Z' }

const page: CategoryPage = {
  content: [category],
  pageDetails: { page: 0, size: 20, totalPages: 1, totalElements: 1, isLast: true }
}

const response = <T>(data: T, status = 200): AxiosResponse<T> =>
  ({
    data,
    status,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() }
  }) as AxiosResponse<T>

type RequestConfig = {
  params?: Record<string, unknown>
  validateStatus: (status: number) => boolean
  signal?: AbortSignal
}

const configOf = (mock: { mock: { calls: unknown[][] } }): RequestConfig => mock.mock.calls[0][1] as RequestConfig

afterEach(() => {
  vi.restoreAllMocks()
})

describe('category.api', () => {
  describe('getCategories', () => {
    it('calls GET /v1/categories dropping undefined params and returns the parsed page', async () => {
      const get = vi.spyOn(categoryApiClient, 'get').mockResolvedValue(response(page))
      const controller = new AbortController()

      const result = await getCategories(
        { page: 0, size: 100, sortBy: 'name', sortDirection: undefined },
        controller.signal
      )

      expect(result).toEqual(page)
      expect(get).toHaveBeenCalledWith(CATEGORIES_ENDPOINT, expect.any(Object))
      const config = configOf(get)
      expect(config.params).toEqual({ page: 0, size: 100, sortBy: 'name' })
      expect(config.signal).toBe(controller.signal)
      expect(config.validateStatus(200)).toBe(true)
      expect(config.validateStatus(400)).toBe(false)
    })

    it('sends no params by default', async () => {
      const get = vi.spyOn(categoryApiClient, 'get').mockResolvedValue(response(page))

      await getCategories()

      expect(configOf(get).params).toEqual({})
    })

    it.each<[string, CategoryListParams]>([
      ['page above 1000', { page: 1001 }],
      ['negative page', { page: -1 }],
      ['size above 100', { size: 101 }],
      ['size of zero', { size: 0 }],
      ['unsupported sortBy', { sortBy: 'price' as never }],
      ['unknown param', { search: 'x' } as never]
    ])('rejects %s before sending the request', async (_, params) => {
      const get = vi.spyOn(categoryApiClient, 'get')

      await expect(getCategories(params)).rejects.toBeInstanceOf(ZodError)
      expect(get).not.toHaveBeenCalled()
    })

    it('rejects a response that does not match the contract', async () => {
      vi.spyOn(categoryApiClient, 'get').mockResolvedValue(response({ content: [{ id: '1' }] }))

      await expect(getCategories()).rejects.toBeInstanceOf(ZodError)
    })
  })

  describe('getCategoryById', () => {
    it('calls GET /v1/categories/{id} and returns the category', async () => {
      const get = vi.spyOn(categoryApiClient, 'get').mockResolvedValue(response(category))

      await expect(getCategoryById(5)).resolves.toEqual(category)
      expect(get).toHaveBeenCalledWith(`${CATEGORIES_ENDPOINT}/5`, expect.any(Object))
      expect(configOf(get).validateStatus(404)).toBe(false)
    })
  })

  describe('getCategoryByName', () => {
    it('URL-encodes the name', async () => {
      const get = vi
        .spyOn(categoryApiClient, 'get')
        .mockResolvedValue(response({ ...category, name: 'Food & Beverage' }))

      const result = await getCategoryByName('Food & Beverage')

      expect(result.name).toBe('Food & Beverage')
      expect(get).toHaveBeenCalledWith(`${CATEGORIES_ENDPOINT}/name/Food%20%26%20Beverage`, expect.any(Object))
      expect(configOf(get).validateStatus(200)).toBe(true)
    })
  })
})
