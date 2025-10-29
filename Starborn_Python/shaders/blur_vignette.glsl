#ifdef GL_ES
precision highp float;
#endif

/* Outputs from the vertex shader */
varying vec4 frag_color;
varying vec2 tex_coord0;

/* uniform texture samplers */
uniform sampler2D texture0;

uniform float vignette_intensity;
uniform float blur_intensity;

void main()
{
    vec2 uv = tex_coord0;
    vec4 color = texture2D(texture0, uv);

    // Apply vertical blur
    float blur = blur_intensity * 0.01;
    color += texture2D(texture0, vec2(uv.x, uv.y - 4.0 * blur));
    color += texture2D(texture0, vec2(uv.x, uv.y - 3.0 * blur));
    color += texture2D(texture0, vec2(uv.x, uv.y - 2.0 * blur));
    color += texture2D(texture0, vec2(uv.x, uv.y - 1.0 * blur));
    color += texture2D(texture0, vec2(uv.x, uv.y + 1.0 * blur));
    color += texture2D(texture0, vec2(uv.x, uv.y + 2.0 * blur));
    color += texture2D(texture0, vec2(uv.x, uv.y + 3.0 * blur));
    color += texture2D(texture0, vec2(uv.x, uv.y + 4.0 * blur));
    color /= 9.0;

    // Apply vignette
    float vignette = 1.0 - distance(uv, vec2(0.5, 0.5)) * vignette_intensity;
    color.rgb *= vignette;

    gl_FragColor = frag_color * color;
}