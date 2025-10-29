#ifdef GL_ES
precision highp float;
#endif

/* Declare varyings used by both stages */
varying vec4 frag_color;
varying vec2 tex_coord0;

/* Default Kivy vertex stage when not overridden */
#ifdef VERTEX
attribute vec2 vVertex;
attribute vec2 vTexCoords0;
attribute vec4 vColor;
uniform mat4 modelview_mat;
uniform mat4 projection_mat;

void main(void)
{
    tex_coord0 = vTexCoords0;
    frag_color = vColor;
    gl_Position = projection_mat * modelview_mat * vec4(vVertex, 0.0, 1.0);
}
#endif

/* Fragment stage */
#ifdef FRAGMENT

uniform sampler2D texture0;
uniform float blur_intensity;
uniform float fade_start;  // Y-coordinate (0.0-1.0) where fading starts
uniform float fade_end;    // Y-coordinate (0.0-1.0) where fading ends (fully transparent)
uniform float fade_power;  // Controls the curve of the fade (1.0 linear)
uniform vec2 uv_offset;    // Offset into source texture
uniform vec2 uv_scale;     // Scale into source texture (map overlay quad to a band)
uniform float fade_flip;   // 0.0: use t, 1.0: use 1-t

void main(void)
{
    // Map overlay texcoords into a thin source band near an edge
    vec2 band_uv = uv_offset + tex_coord0 * uv_scale;
    vec4 color = texture2D(texture0, band_uv);

    // Apply vertical blur
    float blur = blur_intensity * 0.005; // Reduced intensity for finer control
    color = texture2D(texture0, vec2(band_uv.x, band_uv.y - 4.0 * blur));
    color += texture2D(texture0, vec2(band_uv.x, band_uv.y - 3.0 * blur));
    color += texture2D(texture0, vec2(band_uv.x, band_uv.y - 2.0 * blur));
    color += texture2D(texture0, vec2(band_uv.x, band_uv.y - 1.0 * blur));
    color += texture2D(texture0, vec2(band_uv.x, band_uv.y)); // Original pixel weight
    color += texture2D(texture0, vec2(band_uv.x, band_uv.y + 1.0 * blur));
    color += texture2D(texture0, vec2(band_uv.x, band_uv.y + 2.0 * blur));
    color += texture2D(texture0, vec2(band_uv.x, band_uv.y + 3.0 * blur));
    color += texture2D(texture0, vec2(band_uv.x, band_uv.y + 4.0 * blur));
    color /= 9.0;

    // Compute fade factor based on overlay-local vertical coordinate
    float alpha_factor = 1.0;
    if (fade_start != fade_end) {
        float t = tex_coord0.y; // 0 at overlay bottom, 1 at overlay top
        // For top overlay (fade_flip = 1), use t so alpha=1 near top.
        // For bottom overlay (fade_flip = 0), use (1 - t) so alpha=1 near bottom.
        float edge_t = mix(1.0 - t, t, clamp(fade_flip, 0.0, 1.0));
        float e0 = min(fade_start, fade_end);
        float e1 = max(fade_start, fade_end);
        alpha_factor = smoothstep(e0, e1, edge_t);
    }
    // Optional curve control
    alpha_factor = clamp(alpha_factor, 0.0, 1.0);
    alpha_factor = pow(alpha_factor, max(0.0001, fade_power));

    color.a *= alpha_factor; // Apply fade to alpha
    gl_FragColor = frag_color * color;
}
#endif
