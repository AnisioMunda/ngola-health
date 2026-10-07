const currencyFormatter = new Intl.NumberFormat('pt-AO', {
  style: 'currency',
  currency: 'AOA',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const compactCurrencyFormatter = new Intl.NumberFormat('pt-AO', {
  style: 'currency',
  currency: 'AOA',
  notation: 'compact',
  maximumFractionDigits: 1,
});

export function formatAoaCurrency(value: number): string {
  return currencyFormatter.format(value);
}

export function formatAoaCompactCurrency(value: number): string {
  return compactCurrencyFormatter.format(value);
}
