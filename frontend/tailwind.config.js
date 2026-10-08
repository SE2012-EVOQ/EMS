/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      fontFamily: { sans: ['"Plus Jakarta Sans"', 'sans-serif'] },
      colors: {
        app: {
          bg: '#F4F4F6', card: '#FFFFFF', subtle: '#F7F7F8', border: '#EFEFEF',
          text: '#1A1D1F', muted: '#6F767E', green: '#22764F', 'green-bg': '#EAF7EE',
          pink: '#B93E50', 'pink-bg': '#FDF0EE', amber: '#93600D', 'amber-bg': '#FFF7DF',
          blue: '#4165D5', 'blue-bg': '#EEF2FF'
        }
      },
      boxShadow: {
        card: '0 8px 30px rgba(0,0,0,.04)',
        'card-hover': '0 14px 34px rgba(0,0,0,.07)'
      }
    }
  },
  plugins: []
}
