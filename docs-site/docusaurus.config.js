// @ts-check
// `@type` JSDoc annotations allow editor autocompletion and type checking
// (when paired with `@ts-check`).
// There are various equivalent ways to declare your Docusaurus config.
// See: https://docusaurus.io/docs/api/docusaurus-config

import {themes as prismThemes} from 'prism-react-renderer';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Practikalia',
  tagline: 'Documentación técnica',

  future: {
    v4: true, // Improve compatibility with the upcoming Docusaurus v4
  },

  url: 'https://sdurutr436.github.io',
  baseUrl: '/practikalia/',

  organizationName: 'sdurutr436',
  projectName: 'practikalia',

  onBrokenLinks: 'throw',

  i18n: {
    defaultLocale: 'es',
    locales: ['es'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          routeBasePath: '/', // la documentación es la portada, sin plugin de páginas ni blog
          sidebarPath: './sidebars.js',
          editUrl: 'https://github.com/sdurutr436/practikalia/tree/desarrollo/docs-site/',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      colorMode: {
        respectPrefersColorScheme: true,
      },
      navbar: {
        title: 'Practikalia',
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'docsSidebar',
            position: 'left',
            label: 'Documentación',
          },
          {
            href: 'https://github.com/sdurutr436/practikalia',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Proyecto',
            items: [
              {label: 'Repositorio', href: 'https://github.com/sdurutr436/practikalia'},
              {label: 'Licencia (MIT)', href: 'https://github.com/sdurutr436/practikalia/blob/main/LICENSE'},
            ],
          },
        ],
        copyright: `Practikalia — proyecto open source bajo licencia MIT.`,
      },
      prism: {
        theme: prismThemes.github,
        darkTheme: prismThemes.dracula,
      },
    }),
};

export default config;
