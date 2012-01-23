varying vec2 vTex;
uniform sampler2D textureUnit0;

void main(void) {
 gl_FragColor = vec4(vTex.x, 1.0, 1.0, 1.0);
 //texture2D(textureUnit0, vTex);
}
