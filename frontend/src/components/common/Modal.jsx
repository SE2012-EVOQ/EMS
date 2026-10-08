import { useEffect, useId, useRef } from 'react'
import { X } from 'lucide-react'

export default function Modal({ open, onClose, title, subtitle, children, footer, size = 'max-w-xl' }) {
  const titleId = useId()
  const subtitleId = useId()
  const panel = useRef(null)
  const close = useRef(onClose)
  close.current = onClose

  useEffect(() => {
    if (!open) return
    const previousFocus = document.activeElement
    const dialog = panel.current
    const focusable = () => [...dialog.querySelectorAll('button, a[href], input, select, textarea, [tabindex="0"]')]
      .filter(element => !element.disabled && element.getClientRects().length)
    dialog.focus()
    const handleKey = event => {
      // Only the uppermost dialog handles keys when dialogs are stacked.
      const dialogs = document.querySelectorAll('[role="dialog"]')
      if (dialogs[dialogs.length - 1] !== dialog) return
      if (event.key === 'Escape') { event.preventDefault(); close.current(); return }
      if (event.key !== 'Tab') return
      const elements = focusable()
      const first = elements[0]
      const last = elements[elements.length - 1]
      if (!first) { event.preventDefault(); dialog.focus(); return }
      if (event.shiftKey && (document.activeElement === first || document.activeElement === dialog)) {
        event.preventDefault(); last.focus()
      } else if (!event.shiftKey && (document.activeElement === last || document.activeElement === dialog)) {
        event.preventDefault(); first.focus()
      }
    }
    document.addEventListener('keydown', handleKey)
    return () => {
      document.removeEventListener('keydown', handleKey)
      if (previousFocus?.isConnected) previousFocus.focus()
    }
  }, [open])

  if (!open) return null
  return (
    <div className="modal-layer fixed inset-0 z-[80] modal-backdrop p-3 sm:p-4 flex items-center justify-center" onMouseDown={event => event.target === event.currentTarget && onClose()}>
      <div ref={panel} role="dialog" aria-modal="true" aria-labelledby={titleId} aria-describedby={subtitle ? subtitleId : undefined} tabIndex={-1}
        className={`modal-panel surface bg-white w-full ${size} max-h-[calc(100dvh-2rem)] flex flex-col min-h-0 overflow-hidden rounded-[24px] shadow-2xl border border-app-border outline-none`}>
        <div className="shrink-0 p-4 sm:p-5 flex items-start justify-between gap-3 border-b border-app-border">
          <div className="min-w-0"><h2 id={titleId} className="text-lg font-extrabold tracking-tight txt break-words">{title}</h2>{subtitle && <p id={subtitleId} className="text-xs text-app-muted muted mt-1 leading-relaxed">{subtitle}</p>}</div>
          <button type="button" onClick={onClose} aria-label="Close dialog" className="w-9 h-9 shrink-0 rounded-full bg-app-subtle subtle flex items-center justify-center"><X className="w-4 h-4" /></button>
        </div>
        <div className="p-4 sm:p-5 overflow-y-auto min-h-0 overscroll-contain">{children}</div>
        {footer && <div className="shrink-0 p-4 sm:p-5 border-t border-app-border flex flex-wrap justify-end gap-2">{footer}</div>}
      </div>
    </div>
  )
}
