# Portfolio Assets

Place professional portfolio images and videos in this folder.

## Naming Convention
- Photos: `{professionalId}_{description}.jpg` — e.g. `1_haircut_before.jpg`
- Videos: `{professionalId}_{description}.mp4` — e.g. `1_bridal_demo.mp4`

## Supported Formats
- Images: `.jpg`, `.jpeg`, `.png`, `.webp`
- Videos: `.mp4`, `.webm`

## How it works
1. Professional places their file here (e.g. `haircut1.jpg`)
2. In the Portfolio form, they enter the path: `/assets/portfolio/haircut1.jpg`
3. The backend stores only this path as metadata
4. Customers see the image loaded directly from this folder
