#version 330
#extension GL_ARB_separate_shader_objects : require

// Rarity stroke (RarityOutline.java). The item and its stroke both use this pipeline (same sort key, so the stroke stays under
// the item). Vertex alpha says what to draw: 255 = the item itself (premultiplied, as vanilla), 254 = its silhouette in the
// vertex colour, 253 = Legendary gold gradient, 252 = Mythic red gradient (both move).

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    float GlintAlpha;
    vec3 CameraOffset;
    float GameTime;
    vec2 ScreenSize;
    int MenuBlurRadius;
    int UseRgss;
};

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 item = texture(Sampler0, texCoord0);
    int mode = int(vertexColor.a * 255.0 + 0.5);
    if (mode == 255) {
        if (item.a == 0.0) {
            discard;
        }
        fragColor = item * ColorModulator;
        return;
    }
    if (item.a < 0.35) {
        discard;
    }
    vec3 color = vertexColor.rgb;
    if (mode < 254) {
        vec3 from = mode == 253 ? vec3(1.0, 0.62, 0.11) : vec3(1.0, 0.20, 0.20);
        vec3 to = mode == 253 ? vec3(1.0, 0.90, 0.55) : vec3(1.0, 0.42, 0.80);
        // GameTime is the day fraction (24000 ticks = 1200 s): a diagonal band sweeps across about every 2.6 s.
        float t = 0.5 + 0.5 * sin((gl_FragCoord.x + gl_FragCoord.y) * 0.07 - GameTime * 1200.0 * 2.4);
        color = mix(from, to, t);
    }
    fragColor = vec4(color, 1.0) * ColorModulator;
}
