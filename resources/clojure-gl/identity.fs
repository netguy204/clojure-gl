varying vec2 vTex;
uniform sampler2D textureUnit0;
uniform float alpha;

void main(void) {
 vec4 color = texture2D(textureUnit0, vTex);
 color.a = color.a * alpha;
 gl_FragColor = color;
}
