import test from 'node:test'
import assert from 'node:assert/strict'
import { reportCsv } from '../src/modules/reports/reportExport.js'

test('module report export preserves scope, current balance facts and empty tables', () => {
  const csv = reportCsv({ scope: 'MINE', summary: { 'Leave requests': 0 }, tables: [{ title: 'Current balances', columns: ['Type', 'Available'], rows: [] }] })
  assert.ok(csv.includes('"Scope","MINE"\r\n'))
  assert.ok(csv.includes('"Leave requests","0"'))
  assert.ok(csv.includes('"Current balances"\r\n"Type","Available"'))
})
test('exports quote text and neutralize formula cells including leading whitespace', () => {
  const csv = reportCsv({ scope: 'MINE', summary: {}, tables: [{ title: 'Assets', columns: ['Name'], rows: [['=1+1'], [' \t@SUM(A1)'], ['a,"quoted"\nname']] }] })
  assert.ok(csv.includes('"\'=1+1"'))
  assert.ok(csv.includes('"\' \t@SUM(A1)"'))
  assert.ok(csv.includes('"a,""quoted""\nname"'))
})
