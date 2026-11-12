import { http, HttpResponse, type AnyHandler } from 'msw'
import { API_BASE_URL } from '../core/http/api-client'
import { CATEGORIES_ENDPOINT } from '../features/category/api/category.api'
import type { CategoryPage } from '../features/category/api/category.dto'

export const CATEGORIES_URL = `${API_BASE_URL}${CATEGORIES_ENDPOINT}`

export const categoriesPage: CategoryPage = {
  content: [
    { id: 1, name: 'Audio', createdAt: '2026-11-11T20:00:00Z' },
    { id: 2, name: 'Electronics', createdAt: '2026-11-11T20:00:00Z' }
  ],
  pageDetails: { page: 0, size: 100, totalPages: 1, totalElements: 2, isLast: true }
}

export const handlers: AnyHandler[] = [http.get(CATEGORIES_URL, () => HttpResponse.json(categoriesPage))]
