import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Cadence Control Center | Adaptive AI Learning Coach & Constraint Scheduler",
  description: "Deterministic Math (OR-Tools CP-SAT + FSRS-4.5) + Local LLM Semantic Translation",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="dark">
      <body className="bg-[#0b0d11] text-slate-100 antialiased selection:bg-sky-500/20 selection:text-sky-300">
        {children}
      </body>
    </html>
  );
}
