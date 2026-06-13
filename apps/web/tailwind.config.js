/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: {
          100: '#e4e4e7',
          200: '#d4d4d8',
          300: '#a1a1aa',
          400: '#71717a',
          500: '#52525b',
          600: '#3f3f46',
          700: '#2c2c33',
          800: '#232329',
          900: '#18181b',
          950: '#111114',
        },
        accent: '#a78bfa',
      },
    },
  },
  plugins: [],
}
