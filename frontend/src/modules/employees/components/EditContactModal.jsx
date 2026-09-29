import React, { useState, useEffect } from 'react'
import Modal from '../../../components/common/Modal'

function FormField({ label, required, children }) {
  return (
    <div className="space-y-1">
      <label className="block text-xs font-semibold text-gray-700">
        {label} {required && <span className="text-red-500">*</span>}
      </label>
      {children}
    </div>
  )
}

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

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)

    try {
      await onSubmit(employee.id, {
        phone: phone.trim() || null,
        address: address.trim() || null
      })
      onClose()
    } catch (err) {
      setError(err.message || 'Failed to update contact info')
    } finally {
      setSubmitting(false)
    }
  }

  if (!employee) return null

  return (
    <Modal open={open} title={`Update Contact Info: ${employee.fullName}`} onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-xs text-red-700 bg-red-50 border border-red-200 rounded-xl">
            {error}
          </div>
        )}

        <FormField label="Phone Number">
          <input
            type="text"
            value={phone}
            onChange={e => setPhone(e.target.value)}
            placeholder="077 123 4567"
            className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500"
          />
        </FormField>

        <FormField label="Residential Address">
          <textarea
            rows={3}
            value={address}
            onChange={e => setAddress(e.target.value)}
            placeholder="Enter home address"
            className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500"
          />
        </FormField>

        <div className="flex justify-end gap-2 pt-3 border-t border-gray-100">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm font-semibold text-gray-600 hover:bg-gray-100 rounded-xl transition"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={submitting}
            className="px-5 py-2 text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded-xl transition"
          >
            {submitting ? 'Updating...' : 'Update Contact Info'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
