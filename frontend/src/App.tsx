import { Link, Route, Routes } from 'react-router-dom'

import AuthGate from './components/AuthGate'
import AddItem from './routes/AddItem'
import ItemDetail from './routes/ItemDetail'
import Stylist from './routes/Stylist'
import WardrobeGrid from './routes/WardrobeGrid'

/**
 * App shell: a persistent header (home link + style/add affordances) wrapping the
 * routed screens. Mobile-first single column; the routes are the wardrobe grid
 * (`/`), stylist (`/style`), add-item (`/add`), and item detail (`/item/:id`).
 * The whole shell sits behind `AuthGate`, which renders the passcode screen until
 * a valid session token is stored.
 */
export default function App() {
  return (
    <AuthGate>
      <div className="app-shell">
        <header className="app-header">
          <Link to="/" className="app-title">
            Ensemble
          </Link>
          <nav className="app-nav">
            <Link to="/style" className="btn">
              Style
            </Link>
            <Link to="/add" className="btn btn-add">
              + Add
            </Link>
          </nav>
        </header>
        <main className="app-main">
          <Routes>
            <Route path="/" element={<WardrobeGrid />} />
            <Route path="/style" element={<Stylist />} />
            <Route path="/add" element={<AddItem />} />
            <Route path="/item/:id" element={<ItemDetail />} />
          </Routes>
        </main>
      </div>
    </AuthGate>
  )
}
