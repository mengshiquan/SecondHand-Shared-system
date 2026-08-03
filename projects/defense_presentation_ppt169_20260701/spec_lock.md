# Execution Lock

> Machine-readable execution contract. Executor MUST `read_file` this before every SVG page. Values NOT listed here must NOT appear in SVGs. For design narrative (rationale, audience, style), see `design_spec.md`.

## canvas
- viewBox: 0 0 1280 720
- format: PPT 16:9

## colors
- bg: #FFFFFF
- bg_secondary: #F8FAFC
- card_bg: #FFFFFF
- primary: #1565C0
- accent: #0D47A1
- secondary_accent: #1976D2
- text: #1E293B
- text_secondary: #64748B
- text_tertiary: #94A3B8
- border: #E2E8F0
- success: #10B981
- warning: #EF4444

## typography
- font_family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif
- cover_title: 96
- title: 56
- content_title: 40
- subtitle: 40
- body: 32
- annotation: 26
- page_number: 19

## icons
- library: chunk-filled
- inventory: users, archive-box, shopping-cart, database, server, globe, lock-closed, heart, message-circle, folder

## images
- cover_bg: images/cover_bg.png
- architecture_p04: images/architecture_p04.png
- feature_p06: images/feature_p06.png

## page_rhythm
- P01: anchor
- P02: breathing
- P03: dense
- P04: dense
- P05: dense
- P06: dense
- P07: dense
- P08: dense
- P09: dense
- P10: anchor

## forbidden
- Mixing icon libraries
- rgba()
- `<style>`, `class`, `<foreignObject>`, `textPath`, `@font-face`, `<animate*>`, `<script>`, `<iframe>`, `<symbol>`+`<use>`
- `<g opacity>` (set opacity on each child element individually)