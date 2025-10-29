
// tint_replace.glsl - Fragment shader for dynamic theme recolour
// Expects icon/source drawn in solid white on transparent background.
//
// Uniforms:
//   sampler2D u_texture   - base texture (icon)
//   vec4      u_tintColor - desired output tint (RGB) with master alpha
// Varyings:
//   vec2      v_texCoord  - UV from vertex shader
//
#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform vec4      u_tintColor;
varying vec2      v_texCoord;

void main() {
    vec4 tex = texture2D(u_texture, v_texCoord);

    // Preserve alpha mask
    float alpha = tex.a * u_tintColor.a;
    if (alpha < 0.01) discard;        // early out for transparency

    // Convert incoming colour to intensity (assume greyscale source)
    float intensity = dot(tex.rgb, vec3(0.3333));

    // Apply tint while keeping original intensity for subtle shading
    vec3 tinted = u_tintColor.rgb * intensity;

    gl_FragColor = vec4(tinted, alpha);
}
