/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./App.{js,jsx,ts,tsx}", "./src/**/*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
      colors: {
        primary: {
          50: "#eff6ff",
          100: "#dbeafe",
          200: "#bfdbfe",
          300: "#93c5fd",
          400: "#60a5fa",
          500: "#3b82f6",
          600: "#2563eb",
          700: "#1d4ed8",
          800: "#1e40af",
          900: "#1e3a8a",
        },
        glass: {
          light: "rgba(255, 255, 255, 0.1)",
          medium: "rgba(255, 255, 255, 0.15)",
          dark: "rgba(255, 255, 255, 0.05)",
        },
        background: {
          start: "#0f172a",
          end: "#1e293b",
        },
        accent: {
          teal: "#14b8a6",
          purple: "#8b5cf6",
          blue: "#3b82f6",
        },
      },
      fontFamily: {
        mono: ["SpaceMono", "monospace"],
      },
    },
  },
  plugins: [],
};
