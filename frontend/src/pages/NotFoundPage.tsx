import { Link } from 'react-router-dom'
import { EmptyState } from '../components/ui'

export function NotFoundPage() {
  return (
    <div className="page-state">
      <EmptyState title="Page not found" hint="The page you are looking for does not exist.">
        <Link to="/" className="btn btn-ghost" style={{ marginTop: 10 }}>
          Back to customer search
        </Link>
      </EmptyState>
    </div>
  )
}
