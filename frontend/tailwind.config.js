/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      fontFamily: { sans: ['"Plus Jakarta Sans"', 'sans-serif'] },
      colors: {
        app: {
          bg: '#F4F4F6', card: '#FFFFFF', subtle: '#F7F7F8', border: '#EFEFEF',
          text: '#1A1D1F', muted: '#6F767E', green: '#2A9D68', 'green-bg': '#EAF7EE',
          pink: '#E8636F', 'pink-bg': '#FDF0EE', amber: '#E4A72C', 'amber-bg': '#FFF7DF',
          blue: '#5A7CF7', 'blue-bg': '#EEF2FF'
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
