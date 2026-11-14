# BarelyStorage listing kit: CurseForge

Everything needed to fill in the CurseForge project form. Files are in `branding/icon/` and `branding/banners/`.

## Project fields

| Field | CurseForge |
|---|---|
| Project ID | not created yet (Casey creates the project; record the id here and in `.github/scripts/publish_curseforge.py`) |
| Name | BarelyStorage |
| Slug / URL | `barelystorage` |
| Mod id | `barelystorage` |
| Icon / logo | `icon/icon_512.png` (square, at least 400x400) |
| Class | Mods |
| Main category | Storage |
| Loaders | Forge and NeoForge |
| Game versions | Minecraft 26.1.2, 26.2 and 26.3, one jar per Minecraft version; set on each uploaded file |
| Environment | Client and Server (the upload script sends both) |
| License | MIT |
| Source | https://github.com/ItMeCasey/BarelyStorage |
| Issues | https://github.com/ItMeCasey/BarelyStorage/issues |
| Wiki | https://itmecasey.com/wiki/barelystorage/ (not deployed yet; add the link once it is live) |

### One-line description

> A server room for your items: racks, drives, terminals and pattern crafting limited by CPU and RAM, for Forge and NeoForge, powered by Forge Energy.

Use it for the CurseForge summary (under 250 characters), social posts and the top of the description.

### Gallery

| File | Use |
|---|---|
| `banners/features.png` (1920x1080) | First gallery image: the blocks |
| `banners/network.png` (1920x1080) | How the network fits together, second gallery image |
| `banners/social.png` (1280x640) | GitHub social preview (repo Settings > Social preview) |
| `banners/banner.png` (800x150, saved at 2x) | Top of the description |
| `banners/h_*.png` (800x46, saved at 2x) | Optional section headers (features, blocks, config, compat, faq); the description uses plain `#` headers |

The panels in these images are illustrations drawn for the branding; the block faces are the mod's own textures. Add real in-game screenshots from the tour frames once they are picked.

### Description

The CurseForge description is in [`curseforge/DESCRIPTION.md`](../curseforge/DESCRIPTION.md). Paste it with the description type set to **Markdown**. Its images load from GitHub, so `branding/` has to be pushed first.

## Checklist

1. Push `branding/` to GitHub so the description images load.
2. Create the CurseForge project with `icon_512.png` as the logo, record the project id above and in the upload script, and add the `CURSEFORGE_TOKEN`, `IMC_MAVEN_USER` and `IMC_MAVEN_PASSWORD` repository secrets.
3. Set `banners/social.png` as the GitHub social preview.
4. `icon/barelystorage_logo.png` is the mod list logo.
