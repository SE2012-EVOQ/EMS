import React, { useState, useEffect } from 'react'
import Modal from '../../../components/common/Modal'

export default function EditContactModal({
  open,
  employee,
  onClose,
  onSubmit
}) {
  const [phone, setPhone] = useState('')
  const [address, setAddress] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (employee) {
      setPhone(employee.phone || '')
      setAddress(employee.address || '')
    }
  }, [employee])

  if (!employee) return null

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)

    try {
      const payload = {
        phone: phone.trim() || null,
        address: address.trim() || null
      }

      await onSubmit(employee.id, payload)
      onClose()
    } catch (err) {
      setError(err.message || 'Failed to update contact details')
    } finally {
      setSubmitting(false)
    }
  }

  const footer = (
    <>
      <button
        type="button"
        onClick={onClose}
        disabled={submitting}
        className="px-4 py-2.5 rounded-full border border-gray-200 text-xs font-bold hover:bg-gray-50 transition"
      >
        Cancel
      </button>
      <button
        type="button"
        onClick={handleSubmit}
        disabled={submitting}
        className="px-5 py-2.5 rounded-full bg-[#1A1D1F] hover:bg-black text-white text-xs font-bold transition disabled:opacity-50"
      >
        {submitting ? 'Saving...' : 'Save'}
      </button>
    </>
  )

  return (
    <Modal
      open={open}
      title="Edit contact details"
      subtitle={`Permitted contact information for ${employee.fullName}`}
      onClose={onClose}
      footer={footer}
      size="max-w-lg"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-xs text-app-pink bg-app-pink-bg rounded-2xl font-semibold">
            {error}
          </div>
        )}

        <label className="block">
          <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
            Phone number
          </span>
          <input
            type="text"
            value={phone}
            onChange={e => setPhone(e.target.value)}
            placeholder="e.g. +94 77 123 4567"
            className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
          />
        </label>

        <label className="block">
          <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
            Home address
          </span>
          <textarea
            rows={3}
            value={address}
            onChange={e => setAddress(e.target.value)}
            placeholder="e.g. Colombo 05"
            className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt resize-none"
          />
        </label>
      </form>
    </Modal>
  )
}
