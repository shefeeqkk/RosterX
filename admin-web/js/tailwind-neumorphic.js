/**
 * RosterX — Tailwind Neumorphic (Soft UI) Theme Extension
 * Bridges Neumorphic CSS design tokens directly into Tailwind utility classes.
 */
if (typeof tailwind !== 'undefined') {
  tailwind.config = {
    theme: {
      extend: {
        colors: {
          nm: {
            surface: 'var(--nm-surface)',
            contrast: 'var(--nm-surface-contrast)',
            main: 'var(--nm-text-main)',
            muted: 'var(--nm-text-muted)',
            light: 'var(--nm-text-light)',
            accent: 'var(--nm-accent-end)'
          }
        },
        boxShadow: {
          'nm-raised': 'var(--nm-elevation-raised)',
          'nm-raised-hover': 'var(--nm-elevation-raised-hover)',
          'nm-raised-subtle': 'var(--nm-elevation-raised-subtle)',
          'nm-inset': 'var(--nm-elevation-inset)',
          'nm-inset-subtle': 'var(--nm-elevation-inset-subtle)',
          'nm-deep-well': 'var(--nm-elevation-deep-well)',
          'nm-floating': 'var(--nm-elevation-floating)',
          'nm-floating-hover': 'var(--nm-elevation-floating-hover)',
          'nm-accent-glow': 'var(--nm-accent-glow)'
        },
        borderRadius: {
          'nm-sm': 'var(--nm-radius-sm)',
          'nm-md': 'var(--nm-radius-md)',
          'nm-lg': 'var(--nm-radius-lg)',
          'nm-full': 'var(--nm-radius-full)'
        }
      }
    }
  };
}
