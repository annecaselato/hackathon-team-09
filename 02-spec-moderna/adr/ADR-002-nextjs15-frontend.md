---
title: "ADR-002: Next.js 15 + TypeScript for Frontend"
status: "Accepted"
date: "2026-04-29"
deciders: ["Rodrigo Armenio (Frontend Dev)", "Erique Costa (Frontend Dev)", "Vitor Filincowsky (Tech Lead)"]
tags: ["stage-2", "adr", "frontend", "nextjs", "react"]
---

# ADR-002: Next.js 15 + TypeScript for Frontend

**Status**: Accepted  
**Date**: 2026-04-29  
**Deciders**: Rodrigo Armenio, Erique Costa (Frontend Devs), Vitor Filincowsky (Tech Lead)

---

## 📖 Context

The current SIFAP interface is a **3270 terminal** (mainframe screen) running via Com*plete 6.1.2 teleprocessing monitor. This interface:

- Requires specialized operator training (40+ hours)
- Supports only green-screen text (no graphical elements)
- Is inaccessible on mobile devices
- Has 66-line display limit per screen
- Cannot display real-time data updates

The modernization requires a web UI supporting 500+ concurrent operators, a compliance audit dashboard, beneficiary management forms, payment history views, and monthly batch status monitoring — all responsive and accessible (WCAG 2.1).

DATACORP mandates: **Next.js 15 (App Router) + TypeScript + Tailwind CSS + shadcn/ui**

---

## ⚖️ Decision

We will use **Next.js 15 (App Router)** with **TypeScript (strict mode)**, **Tailwind CSS**, and **shadcn/ui** component library for the SIFAP 2.0 web frontend.

---

## 💡 Rationale

### Next.js 15 Advantages

- **App Router (Server Components)**: Beneficiary list and payment history pages render on server, delivering pre-rendered HTML — critical for screen reader accessibility and SEO
- **React Server Components (RSC)**: Complex data fetching (180M+ payment history queries) runs server-side, avoiding large JSON payload to client
- **Turbopack**: Dev server 700x faster than Webpack; reduces developer iteration time
- **Built-in API Routes**: `/api/*` routes allow frontend-hosted BFF (Backend for Frontend) pattern
- **Streaming SSR**: Payment history loads incrementally (important for 10M+ record tables)

### TypeScript (Strict Mode) Advantages

- **Type Safety**: Catches null/undefined errors at compile time — prevents runtime crashes in production
- **IntelliSense**: Full IDE autocomplete for domain models (Beneficiary, Payment, Discount) via shared types
- **Refactoring Safety**: Rename CPF field across 50 files without breaking something
- **Domain Modeling**: Interface-first design aligns with EARS requirements (e.g., `interface PaymentStatus extends Enum<'P'|'G'|'E'|'C'|'D'|'X'|'R'>`)

### Tailwind CSS + shadcn/ui Advantages

- **Design Consistency**: shadcn/ui provides accessible components (forms, tables, dialogs) pre-built to WCAG 2.1 AA
- **Dark Mode**: Built-in `dark:` variant replaces all CSS variables for dark mode (REQ-UI-002)
- **Responsive**: `sm:`, `md:`, `lg:` breakpoints cover 320px-2560px (REQ-UI-001)
- **Customizable**: shadcn/ui components are copied into project (not npm dependency) — full control
- **Performance**: PurgeCSS removes unused styles in production — CSS bundle <10KB

### Mapping Legacy 3270 Screens to Modern UI

| Legacy 3270 Screen | Modern Next.js Page |
|---|---|
| CADBENEF (Beneficiary Registration) | `/app/beneficiaries/new/page.tsx` |
| CONSBENF (Beneficiary Consultation) | `/app/beneficiaries/page.tsx` (list + search) |
| RELAUDIT (Audit Report) | `/app/audit/page.tsx` (with date filter) |
| RELPGT (Payment Report) | `/app/payments/page.tsx` (with pagination) |
| Batch job status (terminal) | `/app/batch/page.tsx` (real-time status) |

---

## 📊 Consequences

### Positive

- **Modern UX**: Web interface replaces 3270 terminal; operators trained in hours not days
- **Mobile Support**: Operators can manage beneficiaries from tablets/phones
- **Real-Time**: React Server Actions + polling enable live batch job status monitoring
- **Type Safety**: Shared TypeScript types between frontend API calls and backend DTOs reduces integration bugs
- **Accessibility**: shadcn/ui components pre-tested for WCAG 2.1 compliance (ARIA labels, keyboard navigation)

### Negative / Trade-offs

- **Server/Client boundary complexity**: React 19 Server/Client Components requires discipline; addressed with folder naming conventions (`page.tsx` = server, `*Client.tsx` = client)
- **Full Stack JS context switching**: Developers switch between Java (backend) and TypeScript (frontend); addressed by clear team split (Rodrigo + Erique = frontend, Danilo = backend)
- **Bundle optimization**: Large shadcn/ui import risk; mitigated by tree-shaking and component-level imports

---

## 🔀 Alternatives Considered

### Alternative A: React (CRA / Vite) + TypeScript
- **Pros**: Simpler than Next.js, no SSR complexity
- **Rejected because**: No built-in SSR/server components; 180M payment records require server-side data fetching; DATACORP mandate is Next.js 15

### Alternative B: Angular 18
- **Pros**: Strong typing, opinionated structure, good for enterprise
- **Rejected because**: Not DATACORP standard; longer learning curve; heavier bundle size vs. React

### Alternative C: Vue 3 + Nuxt
- **Pros**: Gentle learning curve, good SSR support
- **Rejected because**: Smaller ecosystem for enterprise auth integrations; DATACORP standard is Next.js

### Alternative D: Remix
- **Pros**: Excellent form handling, web standards focused
- **Rejected because**: Smaller community, less mature; DATACORP standard

---

## 🛡️ Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Server/Client component boundary errors | MEDIUM | MEDIUM | Linting rules enforce naming convention; Copilot review |
| WCAG 2.1 violations in custom components | LOW | MEDIUM | axe DevTools automated scan in CI/CD pipeline |
| Next.js 15 breaking changes vs. 14 | LOW | LOW | Lock to 15.x.x in package.json; changelog monitoring |
| Large table performance (180M payments) | MEDIUM | HIGH | Always paginate (REQ-UI-005); server-side filter + React virtualizer for client list |

---

## 🔗 References

- [Next.js 15 App Router Documentation](https://nextjs.org/docs/app)
- [shadcn/ui Components](https://ui.shadcn.com/)
- [REQ-UI-001 through REQ-UI-005](../SPECIFICATION.md)
- [WCAG 2.1 Level AA Checklist](https://www.w3.org/WAI/WCAG21/quickref/)

---

## ✅ Approval

| Role | Name | Status |
|---|---|---|
| Frontend Developer | Rodrigo Armenio | ✅ Approved |
| Frontend Developer | Erique Costa | ✅ Approved |
| Tech Lead | Vitor Filincowsky | ✅ Approved |
