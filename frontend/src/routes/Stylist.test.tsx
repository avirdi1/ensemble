import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import Stylist from './Stylist'
import type { Outfit } from '../api/style'

// The route must never touch the network in tests; mock the API module.
vi.mock('../api/style', () => ({
  requestStyle: vi.fn(),
  photoUrl: (id: string) => `/api/items/${id}/photo`,
}))

import { requestStyle } from '../api/style'

const requestStyleMock = vi.mocked(requestStyle)

const outfit: Outfit = {
  itemIds: ['a', 'b'],
  reason: 'A navy top over slim denim reads clean and modern.',
  items: [
    { itemId: 'a', photoUrl: '/api/items/a/photo' },
    { itemId: 'b', photoUrl: '/api/items/b/photo' },
  ],
}

function renderStylist() {
  return render(
    <MemoryRouter initialEntries={['/style']}>
      <Stylist />
    </MemoryRouter>,
  )
}

/** Types a vibe into the prompt input and submits the form. */
async function submitVibe(user: ReturnType<typeof userEvent.setup>, vibe = 'streetwear today') {
  await user.type(screen.getByRole('textbox', { name: /vibe/i }), vibe)
  await user.click(screen.getByRole('button', { name: /style me/i }))
}

beforeEach(() => {
  requestStyleMock.mockReset()
})

afterEach(() => {
  vi.clearAllMocks()
})

describe('Stylist route', () => {
  it('submits the vibe, shows a loading state, then renders the outfit card with photos + reason', async () => {
    let resolve!: (o: Outfit) => void
    requestStyleMock.mockReturnValue(
      new Promise<Outfit>((r) => {
        resolve = r
      }),
    )
    const user = userEvent.setup()

    renderStylist()
    await submitVibe(user)

    // Loading appears while the request is in flight.
    expect(screen.getByText(/styling/i)).toBeInTheDocument()

    resolve(outfit)

    // The reason renders as editorial copy, and each id renders its real photo.
    expect(await screen.findByText(outfit.reason)).toBeInTheDocument()
    const photos = screen.getAllByRole('img')
    expect(photos).toHaveLength(2)
    expect(photos[0]).toHaveAttribute('src', '/api/items/a/photo')
    expect(requestStyleMock).toHaveBeenCalledWith('streetwear today')
  })

  it('shows a non-crashing error state with a retry that re-requests the same vibe', async () => {
    requestStyleMock.mockRejectedValueOnce(new Error('upstream down'))
    const user = userEvent.setup()

    renderStylist()
    await submitVibe(user)

    expect(await screen.findByText(/couldn.t style/i)).toBeInTheDocument()

    // Retry succeeds → the card renders without re-typing the vibe.
    requestStyleMock.mockResolvedValueOnce(outfit)
    await user.click(screen.getByRole('button', { name: /try again/i }))

    await waitFor(() => expect(screen.getByText(outfit.reason)).toBeInTheDocument())
    expect(requestStyleMock).toHaveBeenCalledTimes(2)
    expect(requestStyleMock).toHaveBeenLastCalledWith('streetwear today')
  })

  it('shows a friendly empty state (no photos) when the wardrobe is too small to style', async () => {
    requestStyleMock.mockResolvedValueOnce({
      itemIds: [],
      reason: 'Add a few more pieces and I can build you a look.',
      items: [],
    })
    const user = userEvent.setup()

    renderStylist()
    await submitVibe(user)

    expect(
      await screen.findByText(/add a few more pieces and i can build you a look\./i),
    ).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
  })
})
